"""
Adaptive Origami Profiles v0.1: named fold profiles that influence
planner, structural aggressiveness, byte fold routing, package compactness.
"""

from infold.profiles.definitions import (
    VALID_PROFILES,
    get_profile_config,
    apply_profile,
)
from infold.profiles.selector import select_profile_auto

__all__ = [
    "VALID_PROFILES",
    "get_profile_config",
    "apply_profile",
    "select_profile_auto",
]
