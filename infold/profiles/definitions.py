"""
Profile definitions: named configuration bundles for fold behavior.

Profiles: sparrow, fox, dragon, golem, serpent.
auto is a selection mode, not a fixed profile.
"""

from __future__ import annotations

from typing import Any

VALID_PROFILES = frozenset({"sparrow", "fox", "dragon", "golem", "serpent"})
AUTO_PROFILE = "auto"


def get_profile_config(profile_name: str) -> dict[str, Any]:
    """
    Return config overrides for the given profile.
    Fox is the balanced baseline; others are variations.
    """
    if profile_name not in VALID_PROFILES:
        raise ValueError(f"Invalid profile: {profile_name}. Valid: {sorted(VALID_PROFILES)}")

    # Fox: balanced baseline (no overrides, use config defaults)
    if profile_name == "fox":
        return {}

    # Sparrow: tiny/overhead-sensitive - compact, conservative
    if profile_name == "sparrow":
        return {
            "package_export": {
                "compact": True,
                "report_text": False,
                "inventory_minimal": True,
            },
            "planner": {
                "min_net_value": 0.5,
                "metadata_penalty_weight": -0.002,
            },
            "thresholds": {
                "metadata_table_fold": {"min_net_gain_bytes": 16},
            },
        }

    # Dragon: large structure-rich - broader analysis, aggressive metadata fold
    if profile_name == "dragon":
        return {
            "package_export": {
                "compact": False,
                "report_text": True,
                "inventory_minimal": False,
            },
            "planner": {
                "min_net_value": -1.0,
                "metadata_penalty_weight": -0.0005,
            },
            "thresholds": {
                "template_skeleton": {"min_scaffold_similarity": 0.78, "max_slot_ratio": 0.38},
                "metadata_table_fold": {"min_net_gain_bytes": 16},
            },
        }

    # Golem: binary/opaque-heavy - chunk-first bias, reduced structural optimism
    if profile_name == "golem":
        return {
            "package_export": {
                "compact": True,
                "report_text": False,
                "inventory_minimal": True,
            },
            "planner": {
                "min_net_value": 0.0,
                "metadata_penalty_weight": -0.0015,
            },
            "thresholds": {
                "byte_fold": {"low_structure_confidence": 0.6},
                "metadata_table_fold": {"min_net_gain_bytes": 24},
            },
        }

    # Serpent: lineage/snapshot-oriented - reuse-friendly defaults
    if profile_name == "serpent":
        return {
            "package_export": {
                "compact": True,
                "report_text": True,
                "inventory_minimal": False,
            },
            "planner": {
                "min_net_value": -0.3,
                "metadata_penalty_weight": -0.0008,
            },
            "thresholds": {
                "metadata_table_fold": {"min_net_gain_bytes": 24},
            },
        }

    return {}


def _deep_merge(base: dict, overrides: dict) -> dict:
    """Merge overrides into base. Overrides take precedence. Nested dicts merged recursively."""
    result = dict(base)
    for k, v in overrides.items():
        if k in result and isinstance(result[k], dict) and isinstance(v, dict):
            result[k] = _deep_merge(result[k], v)
        else:
            result[k] = v
    return result


def apply_profile(config: dict[str, Any], profile_name: str) -> dict[str, Any]:
    """
    Apply profile overrides to config. Returns new config (does not mutate input).
    """
    if profile_name not in VALID_PROFILES:
        raise ValueError(f"Invalid profile: {profile_name}")
    overrides = get_profile_config(profile_name)
    if not overrides:
        return dict(config)
    return _deep_merge(config, overrides)
