"""Python parser: imports, classes, functions, assignments, docstrings, names."""

import ast
import hashlib
from typing import Any

from infold.parsers.base import ParserResult


def _hash_tokens(tokens: list[dict[str, Any]]) -> str:
    """Compute hash of token stream for fingerprinting."""
    parts = [f"{t.get('type','')}:{t.get('value','')}" for t in tokens]
    return hashlib.sha256("|".join(parts).encode()).hexdigest()


def _extract_imports(tree: ast.AST) -> list[dict[str, Any]]:
    """Extract import statements from AST."""
    imports: list[dict[str, Any]] = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                imports.append({
                    "module": alias.name,
                    "names": [alias.name],
                    "asname": alias.asname,
                    "line": node.lineno or 0,
                })
        elif isinstance(node, ast.ImportFrom):
            module = node.module or ""
            names = [a.name for a in node.names]
            imports.append({
                "module": module,
                "names": names,
                "line": node.lineno or 0,
            })
    return imports


def _extract_symbols(tree: ast.AST) -> list[dict[str, Any]]:
    """Extract classes, functions, assignments, and names."""
    symbols: list[dict[str, Any]] = []
    for node in ast.walk(tree):
        if isinstance(node, ast.ClassDef):
            symbols.append({"name": node.name, "kind": "class", "line": node.lineno or 0})
        elif isinstance(node, ast.FunctionDef):
            symbols.append({"name": node.name, "kind": "function", "line": node.lineno or 0})
        elif isinstance(node, ast.Assign):
            for target in node.targets:
                if isinstance(target, ast.Name):
                    symbols.append({"name": target.id, "kind": "assignment", "line": node.lineno or 0})
        elif isinstance(node, ast.Name) and isinstance(node.ctx, ast.Load):
            symbols.append({"name": node.id, "kind": "name", "line": node.lineno or 0})
    return symbols


def _ast_to_tokens(tree: ast.AST, source: str) -> list[dict[str, Any]]:
    """Convert AST to simplified token stream (names, keywords, literals)."""
    tokens: list[dict[str, Any]] = []
    for node in ast.walk(tree):
        if isinstance(node, ast.Name):
            tokens.append({
                "type": "name",
                "value": node.id,
                "line": node.lineno or 0,
                "col": node.col_offset or 0,
            })
        elif isinstance(node, ast.Constant):
            val = str(node.value)[:50]  # truncate long strings
            tokens.append({
                "type": "constant",
                "value": val,
                "line": node.lineno or 0,
                "col": node.col_offset or 0,
            })
        elif isinstance(node, (ast.ClassDef, ast.FunctionDef)):
            tokens.append({
                "type": "def",
                "value": node.name,
                "line": node.lineno or 0,
                "col": node.col_offset or 0,
            })
    return tokens


def parse_python(raw_text: str, *, strict: bool = False) -> ParserResult:
    """
    Parse Python source. Returns tokens, AST, symbols, imports, diagnostics.

    Python coverage: imports, classes, functions, assignments, docstrings, names.
    """
    diagnostics: list[dict[str, Any]] = []
    try:
        tree = ast.parse(raw_text)
        imports = _extract_imports(tree)
        symbols = _extract_symbols(tree)
        tokens = _ast_to_tokens(tree, raw_text)
        token_hash = _hash_tokens(tokens)
        return ParserResult(
            tokens=tokens,
            ast_data=tree,
            symbols=symbols,
            imports=imports,
            token_hash=token_hash,
            parse_success=True,
            parser_confidence=1.0,
            diagnostics=[],
        )
    except SyntaxError as e:
        diagnostics.append({
            "type": "syntax_error",
            "message": str(e.msg),
            "line": e.lineno or 0,
            "offset": e.offset or 0,
        })
        return ParserResult(
            tokens=[],
            ast_data=None,
            symbols=[],
            imports=[],
            token_hash=None,
            parse_success=False,
            parser_confidence=0.0,
            diagnostics=diagnostics,
        )
    except Exception as e:
        diagnostics.append({"type": "parse_error", "message": str(e)})
        return ParserResult(
            tokens=[],
            ast_data=None,
            symbols=[],
            imports=[],
            token_hash=None,
            parse_success=False,
            parser_confidence=0.0,
            diagnostics=diagnostics,
        )
