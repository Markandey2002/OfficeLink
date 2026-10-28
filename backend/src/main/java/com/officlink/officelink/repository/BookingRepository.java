package com.officlink.officelink.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.officlink.officelink.domain.Booking;
import com.officlink.officelink.domain.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

	List<Booking> findByUserIdAndStatusOrderByStartUtcDesc(Long userId, BookingStatus status);

	List<Booking> findByStatusOrderByStartUtcDesc(BookingStatus status);

	/**
	 * Overlap: existing.start &lt; requestedEnd AND existing.end &gt; requestedStart
	 * (half-open intervals [start, end)).
	 */
	@Query("""
			select count(b) > 0 from Booking b
			where b.resource.id = :resourceId
			  and b.status = com.officlink.officelink.domain.BookingStatus.CONFIRMED
			  and b.startUtc < :endUtc
			  and b.endUtc > :startUtc
			  and (:excludeId is null or b.id <> :excludeId)
			""")
	boolean existsOverlapping(
			@Param("resourceId") Long resourceId,
			@Param("startUtc") Instant startUtc,
			@Param("endUtc") Instant endUtc,
			@Param("excludeId") Long excludeId);
}
