package com.officlink.officelink.dto;

import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(
		@NotNull Long resourceId,
		@NotNull Instant startUtc,
		@NotNull Instant endUtc) {
}
