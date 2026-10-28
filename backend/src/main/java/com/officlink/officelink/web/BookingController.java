package com.officlink.officelink.web;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.officlink.officelink.dto.BookingResponse;
import com.officlink.officelink.dto.CreateBookingRequest;
import com.officlink.officelink.security.UserPrincipal;
import com.officlink.officelink.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bookings")
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class BookingController {

	private final BookingService bookingService;

	public BookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@PostMapping
	public BookingResponse create(
			@Valid @RequestBody CreateBookingRequest request,
			@AuthenticationPrincipal UserPrincipal principal) {
		return bookingService.create(request, principal);
	}

	@GetMapping("/mine")
	public List<BookingResponse> mine(@AuthenticationPrincipal UserPrincipal principal) {
		return bookingService.myBookings(principal);
	}

	/** Real-time conflict probe used by the UI before committing a booking. */
	@GetMapping("/conflicts")
	public Map<String, Object> conflicts(
			@RequestParam Long resourceId,
			@RequestParam Instant startUtc,
			@RequestParam Instant endUtc,
			@RequestParam(required = false) Long excludeBookingId) {
		boolean conflict = bookingService.hasConflict(resourceId, startUtc, endUtc, excludeBookingId);
		return Map.of(
				"resourceId", resourceId,
				"startUtc", startUtc.toString(),
				"endUtc", endUtc.toString(),
				"conflict", conflict);
	}

	@DeleteMapping("/{id}")
	public BookingResponse cancel(
			@PathVariable Long id,
			@AuthenticationPrincipal UserPrincipal principal) {
		return bookingService.cancel(id, principal);
	}
}
