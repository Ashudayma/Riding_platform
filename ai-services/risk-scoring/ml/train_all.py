from __future__ import annotations

import subprocess
import sys
from datetime import datetime, timezone

from ml.feature_schemas import FEATURE_SCHEMAS


def main() -> None:
    version = datetime.now(timezone.utc).strftime("baseline-%Y%m%d%H%M%S")
    for use_case in FEATURE_SCHEMAS:
        subprocess.run(
            [sys.executable, "-m", "ml.train", "--use-case", use_case, "--version", version],
            check=True,
        )


if __name__ == "__main__":
    main()
