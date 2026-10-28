package com.officlink.officelink;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.officlink.officelink.domain.AppUser;
import com.officlink.officelink.domain.Booking;
import com.officlink.officelink.domain.BookingStatus;
import com.officlink.officelink.domain.Resource;
import com.officlink.officelink.domain.Role;
import com.officlink.officelink.dto.CreateBookingRequest;
import com.officlink.officelink.repository.AppUserRepository;
import com.officlink.officelink.repository.BookingRepository;
import com.officlink.officelink.repository.ResourceRepository;
import com.officlink.officelink.security.UserPrincipal;
import com.officlink.officelink.service.BookingService;
import com.officlink.officelink.web.error.ApiException;

@SpringBootTest
@TestPropertySource(properties = {
		"spring.datasource.url=jdbc:postgresql://localhost:5432/office_link",
		"spring.datasource.username=postgres",
		"spring.datasource.password=5189",
		"app.oidc.enabled=false"
})
class BookingConcurrencyTest {

	@Autowired
	private BookingService bookingService;
	@Autowired
	private ResourceRepository resourceRepository;
	@Autowired
	private AppUserRepository userRepository;
	@Autowired
	private BookingRepository bookingRepository;

	/** Track bookings created by this test so they can be cleaned up afterwards. */
	private final List<Long> createdBookingIds = new ArrayList<>();

	/**
	 * Delete any bookings this test inserted so subsequent runs start with a clean slate.
	 * This prevents "already booked" false-positives on re-runs without a full DB wipe.
	 */
	@AfterEach
	void cleanup() {
		for (Long id : createdBookingIds) {
			bookingRepository.findById(id).ifPresent(b -> {
				b.setStatus(BookingStatus.CANCELLED);
				bookingRepository.save(b);
			});
		}
		createdBookingIds.clear();
	}

	@Test
	void concurrentOverlappingBookings_onlyOneSucceeds() throws Exception {
		Resource resource = resourceRepository.findByActiveTrueOrderByNameAsc().get(0);
		AppUser userA = userRepository.findByEmailIgnoreCase("user@officlink.com").orElseThrow();
		AppUser userB = userRepository.findByEmailIgnoreCase("admin@officlink.com").orElseThrow();

		// Use a far-future window to avoid conflicts with any existing bookings.
		Instant start = Instant.now().plus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MINUTES);
		Instant end = start.plus(90, ChronoUnit.MINUTES);
		CreateBookingRequest request = new CreateBookingRequest(resource.getId(), start, end);

		CountDownLatch ready = new CountDownLatch(2);
		CountDownLatch startGate = new CountDownLatch(1);
		AtomicInteger successes = new AtomicInteger();
		AtomicInteger conflicts = new AtomicInteger();

		ExecutorService pool = Executors.newFixedThreadPool(2);
		List<Future<?>> futures = new ArrayList<>();

		futures.add(pool.submit(() -> attempt(userA, request, ready, startGate, successes, conflicts)));
		futures.add(pool.submit(() -> attempt(userB, request, ready, startGate, successes, conflicts)));

		ready.await();
		startGate.countDown();
		for (Future<?> future : futures) {
			future.get();
		}
		pool.shutdownNow();

		assertThat(successes.get()).isEqualTo(1);
		assertThat(conflicts.get()).isEqualTo(1);
		assertThat(bookingRepository.existsOverlapping(resource.getId(), start, end, null)).isTrue();
	}

	private void attempt(
			AppUser user,
			CreateBookingRequest request,
			CountDownLatch ready,
			CountDownLatch startGate,
			AtomicInteger successes,
			AtomicInteger conflicts) {
		try {
			ready.countDown();
			startGate.await();
			UserPrincipal principal = new UserPrincipal(
					user.getId(), user.getEmail(), "", user.getDisplayName(),
					user.getRole() == null ? Role.USER : user.getRole());
			Booking created = null;
			try {
				// Capture the created booking so @AfterEach can cancel it.
				com.officlink.officelink.dto.BookingResponse resp = bookingService.create(request, principal);
				synchronized (createdBookingIds) {
					createdBookingIds.add(resp.id());
				}
				successes.incrementAndGet();
			} catch (ApiException ex) {
				if (ex.getStatus().value() == 409) {
					conflicts.incrementAndGet();
				} else {
					throw ex;
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(e);
		}
	}
}
