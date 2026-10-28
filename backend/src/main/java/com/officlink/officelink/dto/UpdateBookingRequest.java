package com.officlink.officelink.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record UpdateBookingRequest(
		@NotNull Instant startUtc,
		@NotNull Instant endUtc) {
}
