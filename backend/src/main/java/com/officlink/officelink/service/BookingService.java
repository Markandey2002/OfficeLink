package com.officlink.officelink.service;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.officlink.officelink.domain.Booking;
import com.officlink.officelink.domain.BookingStatus;
import com.officlink.officelink.dto.BookingResponse;
import com.officlink.officelink.dto.CreateBookingRequest;
import com.officlink.officelink.dto.UpdateBookingRequest;
import com.officlink.officelink.repository.AppUserRepository;
import com.officlink.officelink.repository.BookingRepository;
import com.officlink.officelink.repository.ResourceRepository;
import com.officlink.officelink.security.UserPrincipal;
import com.officlink.officelink.web.error.ApiException;

@Service
public class BookingService {

	private final BookingRepository bookingRepository;
	private final ResourceRepository resourceRepository;
	private final AppUserRepository userRepository;

	public BookingService(
			BookingRepository bookingRepository,
			ResourceRepository resourceRepository,
			AppUserRepository userRepository) {
		this.bookingRepository = bookingRepository;
		this.resourceRepository = resourceRepository;
		this.userRepository = userRepository;
	}

	/**
	 * Creates a booking under a short transaction that:
	 * 1) acquires a pessimistic write lock on the resource row
	 * 2) checks for overlapping confirmed bookings
	 * 3) inserts only if free
	 * This serializes concurrent attempts for the same resource and prevents double-booking.
	 */
	@Transactional
	public BookingResponse create(CreateBookingRequest request, UserPrincipal principal) {
		validateWindow(request.startUtc(), request.endUtc());

		var resource = resourceRepository.findByIdForUpdate(request.resourceId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found"));

		if (!resource.isActive()) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Resource is not available");
		}

		boolean conflict = bookingRepository.existsOverlapping(
				resource.getId(), request.startUtc(), request.endUtc(), null);
		if (conflict) {
			throw new ApiException(HttpStatus.CONFLICT, "Resource is already booked for that time window");
		}

		var user = userRepository.findById(principal.getId())
				.orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "User not found"));

		Booking booking = new Booking();
		booking.setResource(resource);
		booking.setUser(user);
		booking.setStartUtc(request.startUtc());
		booking.setEndUtc(request.endUtc());
		booking.setStatus(BookingStatus.CONFIRMED);
		booking.setCreatedAtUtc(Instant.now());

		return BookingResponse.from(bookingRepository.save(booking));
	}

	@Transactional(readOnly = true)
	public List<BookingResponse> myBookings(UserPrincipal principal) {
		return bookingRepository
				.findByUserIdAndStatusOrderByStartUtcDesc(principal.getId(), BookingStatus.CONFIRMED)
				.stream()
				.map(BookingResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public List<BookingResponse> allConfirmed() {
		return bookingRepository.findByStatusOrderByStartUtcDesc(BookingStatus.CONFIRMED).stream()
				.map(BookingResponse::from)
				.toList();
	}

	@Transactional(readOnly = true)
	public boolean hasConflict(Long resourceId, Instant startUtc, Instant endUtc, Long excludeBookingId) {
		validateWindow(startUtc, endUtc);
		if (!resourceRepository.existsById(resourceId)) {
			throw new ApiException(HttpStatus.NOT_FOUND, "Resource not found");
		}
		return bookingRepository.existsOverlapping(resourceId, startUtc, endUtc, excludeBookingId);
	}

	@Transactional
	public BookingResponse cancel(Long bookingId, UserPrincipal principal) {
		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));

		boolean owner = booking.getUser().getId().equals(principal.getId());
		if (!owner && !principal.isAdmin()) {
			throw new ApiException(HttpStatus.FORBIDDEN, "You can only cancel your own bookings");
		}

		if (booking.getStatus() == BookingStatus.CANCELLED) {
			return BookingResponse.from(booking);
		}

		booking.setStatus(BookingStatus.CANCELLED);
		return BookingResponse.from(bookingRepository.save(booking));
	}

	@Transactional
	public BookingResponse updateAsAdmin(Long bookingId, UpdateBookingRequest request) {
		validateWindow(request.startUtc(), request.endUtc());

		Booking booking = bookingRepository.findById(bookingId)
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));

		if (booking.getStatus() != BookingStatus.CONFIRMED) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Only confirmed bookings can be modified");
		}

		var resource = resourceRepository.findByIdForUpdate(booking.getResource().getId())
				.orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Resource not found"));

		boolean conflict = bookingRepository.existsOverlapping(
				resource.getId(), request.startUtc(), request.endUtc(), booking.getId());
		if (conflict) {
			throw new ApiException(HttpStatus.CONFLICT, "Resource is already booked for that time window");
		}

		booking.setStartUtc(request.startUtc());
		booking.setEndUtc(request.endUtc());
		return BookingResponse.from(bookingRepository.save(booking));
	}

	private void validateWindow(Instant start, Instant end) {
		if (!end.isAfter(start)) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "endUtc must be after startUtc");
		}
		if (start.isBefore(Instant.now().minusSeconds(60))) {
			throw new ApiException(HttpStatus.BAD_REQUEST, "Cannot book in the past");
		}
	}
}
