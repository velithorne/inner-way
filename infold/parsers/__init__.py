"""Parsing layer: extracts tokens, syntax trees, symbols, imports, and diagnostics."""

from infold.models.file_node import FileNode
from infold.models.project_sheet import ProjectSheet
from infold.parsers.base import ParserResult
from infold.parsers.python_parser import parse_python
from infold.parsers.text_parser import parse_markdown, parse_text


def parse_project(sheet: ProjectSheet, *, strict: bool = False) -> ProjectSheet:
    """Parse all file nodes in the project sheet. Returns updated sheet."""
    source = sheet.source_path
    parsed_nodes: dict = {}
    for path, node in sheet.file_nodes.items():
        full_path = source / path
        if full_path.exists():
            parsed = parse_file_node(node, strict=strict)
            parsed_nodes[path] = parsed
        else:
            parsed_nodes[path] = node
    sheet.file_nodes = parsed_nodes
    return sheet


def parse_file_node(node: FileNode, *, strict: bool = False) -> FileNode:
    """
    Parse a FileNode and return an updated node with tokens, AST, symbols, imports.
    """
    result: ParserResult
    if node.language == "python":
        result = parse_python(node.raw_text, strict=strict)
    elif node.language == "markdown":
        result = parse_markdown(node.raw_text, strict=strict)
    else:
        # Fallback for javascript, typescript, json, yaml, text
        result = parse_text(node.raw_text, strict=strict)

    return FileNode(
        path=node.path,
        language=node.language,
        raw_text=node.raw_text,
        tokens=result.tokens,
        ast_data=result.ast_data,
        symbols=result.symbols,
        imports=result.imports,
        raw_hash=node.raw_hash,
        token_hash=result.token_hash,
        parser_confidence=result.parser_confidence,
        diagnostics=node.diagnostics + result.diagnostics,
    )


__all__ = [
    "parse_file_node",
    "parse_project",
    "parse_python",
    "parse_text",
    "parse_markdown",
    "ParserResult",
]
