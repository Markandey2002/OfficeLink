package com.officlink.officelink.dto;

import java.time.Instant;

import com.officlink.officelink.domain.Booking;
import com.officlink.officelink.domain.BookingStatus;
import com.officlink.officelink.domain.ResourceType;

public record BookingResponse(
		Long id,
		Long resourceId,
		String resourceName,
		ResourceType resourceType,
		Long userId,
		String userEmail,
		String userDisplayName,
		Instant startUtc,
		Instant endUtc,
		BookingStatus status,
		Instant createdAtUtc) {

	public static BookingResponse from(Booking booking) {
		return new BookingResponse(
				booking.getId(),
				booking.getResource().getId(),
				booking.getResource().getName(),
				booking.getResource().getType(),
				booking.getUser().getId(),
				booking.getUser().getEmail(),
				booking.getUser().getDisplayName(),
				booking.getStartUtc(),
				booking.getEndUtc(),
				booking.getStatus(),
				booking.getCreatedAtUtc());
	}
}
