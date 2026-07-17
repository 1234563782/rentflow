DELETE FROM outbox_events
WHERE event_type = 'review.created'
  AND status = 'PENDING';
