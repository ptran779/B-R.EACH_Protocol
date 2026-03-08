package com.github.ptran779.breach_ptc.ai.api;

import java.util.function.Supplier;

// hybrid Throttle Sensor with self-cleaning and super lazy loading
public class Sensor<T> {
	private T cachedValue;
	private long nextScanTick = -1;
	private final int interval; // How many ticks between scans
	private final Supplier<T> updater;

	public Sensor(Supplier<T> updater, int interval) {
		this.interval = interval;
		this.updater = updater;
	}

	public Sensor(Supplier<T> updater) {
		this(updater, 1);
	}

	/**
	 * Get the value. Automatically recomputes if the interval has passed,
	 * OR if someone manually called markDirty().
	 */
	public T get(int currentTick) {
		if (currentTick >= nextScanTick) {
			update(currentTick);
		}
		return cachedValue;
	}

	/** Manual force: Triggered by a "Panic" event (like getting shot) */
	public void markDirty(int tickCount) {
		this.nextScanTick = tickCount; // Set next scan to 'Right Now'
	}

	public T update(int currentTick) {
		cachedValue = updater.get();
		this.nextScanTick = currentTick + interval;
		return cachedValue;
	}
}