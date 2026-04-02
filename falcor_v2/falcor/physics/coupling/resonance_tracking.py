"""Resonance tracking placeholder."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class ResonancePeak:
    """Detected resonance peak."""

    frequency_hz: float
    amplitude: float
    q_factor: float | None = None


def find_peaks_simple(freq: list[float], amplitude: list[float], threshold: float) -> list[ResonancePeak]:
    """Simple peak detection. Placeholder."""
    peaks: list[ResonancePeak] = []
    for i in range(1, len(amplitude) - 1):
        if amplitude[i] > threshold and amplitude[i] >= amplitude[i - 1] and amplitude[i] >= amplitude[i + 1]:
            peaks.append(ResonancePeak(frequency_hz=freq[i], amplitude=amplitude[i]))
    return peaks
