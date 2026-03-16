"""Tests for unit conversions."""

import pytest
from falcor.core.units import (
    celsius_to_kelvin,
    kelvin_to_celsius,
    rpm_to_rad_s,
    rad_s_to_rpm,
    MU_0,
    EPS_0,
)


def test_celsius_to_kelvin():
    assert abs(celsius_to_kelvin(0) - 273.15) < 1e-6
    assert abs(celsius_to_kelvin(100) - 373.15) < 1e-6


def test_kelvin_to_celsius():
    assert abs(kelvin_to_celsius(273.15) - 0) < 1e-6
    assert abs(kelvin_to_celsius(373.15) - 100) < 1e-6


def test_rpm_rad_s_roundtrip():
    rpm = 60.0
    rad_s = rpm_to_rad_s(rpm)
    back = rad_s_to_rpm(rad_s)
    assert abs(back - rpm) < 1e-6


def test_constants():
    assert MU_0 > 0
    assert EPS_0 > 0
