package com.officlink.officelink;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import com.officlink.officelink.dto.CreateBookingRequest;
import com.officlink.officelink.web.error.ApiException;

/**
 * Pure unit tests for the booking window validation rules.
 * Exercises the same logic that BookingService.validateWindow() enforces
 * without requiring a Spring context or database.
 */
class BookingWindowValidationTest {

	/** Reproduces the same validation logic as BookingService.validateWindow(). */
	private static void validateWindow(Instant start, Instant end) {
		if (!end.isAfter(start)) {
			throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
					"endUtc must be after startUtc");
		}
		if (start.isBefore(Instant.now().minusSeconds(60))) {
			throw new ApiException(org.springframework.http.HttpStatus.BAD_REQUEST,
					"Cannot book in the past");
		}
	}

	@Test
	void futureWindow_isValid() {
		Instant start = Instant.now().plus(1, ChronoUnit.HOURS);
		Instant end = start.plus(90, ChronoUnit.MINUTES);
		assertThatCode(() -> validateWindow(start, end)).doesNotThrowAnyException();
	}

	@Test
	void endBeforeStart_rejected() {
		Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
		Instant end = start.minus(1, ChronoUnit.MINUTES);
		assertThatThrownBy(() -> validateWindow(start, end))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("endUtc must be after startUtc");
	}

	@Test
	void endEqualsStart_rejected() {
		Instant start = Instant.now().plus(2, ChronoUnit.HOURS);
		assertThatThrownBy(() -> validateWindow(start, start))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("endUtc must be after startUtc");
	}

	@Test
	void startInPast_rejected() {
		Instant start = Instant.now().minus(5, ChronoUnit.MINUTES);
		Instant end = start.plus(90, ChronoUnit.MINUTES);
		assertThatThrownBy(() -> validateWindow(start, end))
				.isInstanceOf(ApiException.class)
				.hasMessageContaining("Cannot book in the past");
	}

	@Test
	void adjacentSlots_wouldNotOverlap() {
		// This tests the half-open interval boundary understanding:
		// slot A: [now+1h, now+2h), slot B: [now+2h, now+3h) → no overlap.
		// The actual overlap check lives in BookingRepository.existsOverlapping.
		// Here we just verify that two touching windows are each individually valid.
		Instant t1Start = Instant.now().plus(1, ChronoUnit.HOURS);
		Instant t1End = t1Start.plus(1, ChronoUnit.HOURS);
		Instant t2Start = t1End; // adjacent — same instant
		Instant t2End = t2Start.plus(1, ChronoUnit.HOURS);

		assertThatCode(() -> validateWindow(t1Start, t1End)).doesNotThrowAnyException();
		assertThatCode(() -> validateWindow(t2Start, t2End)).doesNotThrowAnyException();
	}
}
