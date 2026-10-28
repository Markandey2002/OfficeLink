package com.officlink.officelink.web;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.officlink.officelink.dto.BookingResponse;
import com.officlink.officelink.dto.UpdateBookingRequest;
import com.officlink.officelink.service.BookingService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/admin/bookings")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBookingController {

	private final BookingService bookingService;

	public AdminBookingController(BookingService bookingService) {
		this.bookingService = bookingService;
	}

	@GetMapping
	public List<BookingResponse> all() {
		return bookingService.allConfirmed();
	}

	@PutMapping("/{id}")
	public BookingResponse update(
			@PathVariable Long id,
			@Valid @RequestBody UpdateBookingRequest request) {
		return bookingService.updateAsAdmin(id, request);
	}
}
