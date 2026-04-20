import { defaultConfig } from "@riding-platform/config";

export type RealtimeConnectionOptions = {
  accessToken: string;
  onMessage?: (payload: unknown) => void;
  onOpen?: () => void;
  onClose?: () => void;
  onError?: (error: Event) => void;
};

export type StompConnectionOptions = {
  accessToken: string;
  onOpen?: () => void;
  onClose?: () => void;
  onError?: (message: string) => void;
};

export type StompSubscription = {
  unsubscribe: () => void;
};

export type ManagedRealtimeConnection = {
  disconnect: () => void;
};

export type ManagedStompConnection = ManagedRealtimeConnection & {
  subscribe: (destination: string, onMessage: (payload: unknown) => void) => StompSubscription;
};

type PendingSubscription = {
  id: string;
  destination: string;
  onMessage: (payload: unknown) => void;
};

function normalizeWebSocketUrl(): string {
  const url = new URL(defaultConfig.websocketUrl);
  url.protocol = url.protocol === "https:" ? "wss:" : "ws:";
  return url.toString();
}

export function createWebSocketUrl(): string {
  return normalizeWebSocketUrl();
}

export function reconnectDelay(attempt: number): number {
  return Math.min(1000 * 2 ** attempt, 15000);
}

function parseJsonPayload(value: string): unknown {
  try {
    return JSON.parse(value);
  } catch {
    return value;
  }
}

export function connectRealtime(options: RealtimeConnectionOptions): WebSocket {
  const socket = new WebSocket(createWebSocketUrl());
  socket.onopen = () => options.onOpen?.();
  socket.onclose = () => options.onClose?.();
  socket.onerror = (event) => options.onError?.(event);
  socket.onmessage = (event) => {
    options.onMessage?.(parseJsonPayload(String(event.data)));
  };
  return socket;
}

export function connectRealtimeWithReconnect(options: RealtimeConnectionOptions): ManagedRealtimeConnection {
  let socket: WebSocket | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  let attempt = 0;
  let closedManually = false;

  const cleanupTimer = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
  };

  const connect = () => {
    cleanupTimer();
    socket = connectRealtime({
      ...options,
      onOpen: () => {
        attempt = 0;
        options.onOpen?.();
      },
      onClose: () => {
        options.onClose?.();
        if (closedManually) {
          return;
        }
        reconnectTimer = setTimeout(() => {
          attempt += 1;
          connect();
        }, reconnectDelay(attempt));
      },
    });
  };

  connect();

  return {
    disconnect: () => {
      closedManually = true;
      cleanupTimer();
      socket?.close();
    },
  };
}

function createFrame(command: string, headers: Record<string, string>, body = ""): string {
  const headerLines = Object.entries(headers).map(([key, value]) => `${key}:${value}`);
  return `${command}\n${headerLines.join("\n")}\n\n${body}\u0000`;
}

function splitFrames(raw: string): string[] {
  return raw
    .split("\u0000")
    .map((frame) => frame.trim())
    .filter((frame) => frame.length > 0);
}

function parseFrame(frame: string): {
  command: string;
  headers: Record<string, string>;
  body: string;
} {
  const [headerBlock, ...bodyParts] = frame.split("\n\n");
  const headerLines = headerBlock.split("\n").filter(Boolean);
  const [command, ...headersRaw] = headerLines;
  const headers: Record<string, string> = {};

  headersRaw.forEach((line) => {
    const separatorIndex = line.indexOf(":");
    if (separatorIndex <= 0) {
      return;
    }
    headers[line.slice(0, separatorIndex)] = line.slice(separatorIndex + 1);
  });

  return {
    command,
    headers,
    body: bodyParts.join("\n\n"),
  };
}

export function connectStomp(options: StompConnectionOptions): ManagedStompConnection {
  const socket = new WebSocket(createWebSocketUrl());
  const subscriptions = new Map<string, PendingSubscription>();
  let isConnected = false;

  const resubscribe = () => {
    subscriptions.forEach((subscription) => {
      socket.send(
        createFrame("SUBSCRIBE", {
          id: subscription.id,
          destination: subscription.destination,
          ack: "auto",
        }),
      );
    });
  };

  socket.onopen = () => {
    socket.send(
      createFrame("CONNECT", {
        Authorization: `Bearer ${options.accessToken}`,
        "accept-version": "1.2",
        "heart-beat": "10000,10000",
      }),
    );
  };

  socket.onclose = () => {
    isConnected = false;
    options.onClose?.();
  };

  socket.onerror = () => {
    options.onError?.("WebSocket transport error");
  };

  socket.onmessage = (event) => {
    splitFrames(String(event.data)).forEach((rawFrame) => {
      const frame = parseFrame(rawFrame);
      if (frame.command === "CONNECTED") {
        isConnected = true;
        resubscribe();
        options.onOpen?.();
        return;
      }
      if (frame.command === "MESSAGE") {
        const subscriptionId = frame.headers.subscription;
        const subscription = subscriptionId ? subscriptions.get(subscriptionId) : undefined;
        subscription?.onMessage(parseJsonPayload(frame.body));
        return;
      }
      if (frame.command === "ERROR") {
        options.onError?.(frame.body || "STOMP error");
      }
    });
  };

  return {
    subscribe: (destination, onMessage) => {
      const id = `sub-${Math.random().toString(36).slice(2, 10)}`;
      subscriptions.set(id, { id, destination, onMessage });
      if (isConnected) {
        socket.send(
          createFrame("SUBSCRIBE", {
            id,
            destination,
            ack: "auto",
          }),
        );
      }
      return {
        unsubscribe: () => {
          subscriptions.delete(id);
          if (isConnected) {
            socket.send(
              createFrame("UNSUBSCRIBE", {
                id,
              }),
            );
          }
        },
      };
    },
    disconnect: () => {
      if (socket.readyState === WebSocket.OPEN) {
        socket.send(createFrame("DISCONNECT", {}));
      }
      socket.close();
    },
  };
}

export function connectStompWithReconnect(options: StompConnectionOptions): ManagedStompConnection {
  let connection: ManagedStompConnection | null = null;
  let reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  let attempt = 0;
  let closedManually = false;
  const desiredSubscriptions: Array<{
    destination: string;
    onMessage: (payload: unknown) => void;
    active?: StompSubscription;
  }> = [];

  const cleanupTimer = () => {
    if (reconnectTimer) {
      clearTimeout(reconnectTimer);
      reconnectTimer = null;
    }
  };

  const wireSubscriptions = () => {
    if (!connection) {
      return;
    }
    desiredSubscriptions.forEach((subscription) => {
      subscription.active = connection?.subscribe(subscription.destination, subscription.onMessage);
    });
  };

  const connect = () => {
    cleanupTimer();
    connection = connectStomp({
      ...options,
      onOpen: () => {
        attempt = 0;
        wireSubscriptions();
        options.onOpen?.();
      },
      onClose: () => {
        desiredSubscriptions.forEach((subscription) => {
          subscription.active = undefined;
        });
        options.onClose?.();
        if (closedManually) {
          return;
        }
        reconnectTimer = setTimeout(() => {
          attempt += 1;
          connect();
        }, reconnectDelay(attempt));
      },
    });
  };

  connect();

  return {
    subscribe: (destination, onMessage) => {
      const desired: {
        destination: string;
        onMessage: (payload: unknown) => void;
        active?: StompSubscription;
      } = { destination, onMessage };
      desiredSubscriptions.push(desired);
      if (connection) {
        desired.active = connection.subscribe(destination, onMessage);
      }
      return {
        unsubscribe: () => {
          desired.active?.unsubscribe();
          const index = desiredSubscriptions.indexOf(desired);
          if (index >= 0) {
            desiredSubscriptions.splice(index, 1);
          }
        },
      };
    },
    disconnect: () => {
      closedManually = true;
      cleanupTimer();
      connection?.disconnect();
    },
  };
}
