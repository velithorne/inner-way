"""Base parser interface."""

from abc import ABC, abstractmethod
from typing import Any

from core.types import FileSheet


class ParserBase(ABC):
    """Abstract base for file parsers."""

    @abstractmethod
    def language(self) -> str:
        """Supported language identifier."""
        ...

    @abstractmethod
    def parse(self, raw_text: str, path: str = "") -> dict[str, Any]:
        """
        Parse raw text. Returns:
        - raw_text
        - tokens
        - parse_success
        - ast (if supported)
        - symbols (if supported)
        - import_refs (if supported)
        - fingerprints
        - diagnostics
        """
        ...
