package lld.urlShortener.observer;

import lld.urlShortener.analytics.AnalyticsService;

/**
 * Analytics Listener — records click events for every URL access.
 *
 * Concrete Observer. Reacts only to URL_ACCESSED events.
 * Everything else is ignored — no unnecessary processing.
 *
 * SOLID - SRP: Only records analytics — nothing else.
 * SOLID - OCP: UrlEventPublisher never changes to accommodate this listener.
 * SOLID - DIP: Depends on AnalyticsService (interface), not the concrete in-memory impl.
 */
public class AnalyticsEventListener implements UrlEventListener {

    private final AnalyticsService analyticsService;

    public AnalyticsEventListener(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @Override
    public void onEvent(UrlEvent event) {
        if (event.getType() != UrlEventType.URL_ACCESSED) return;

        analyticsService.recordClick(
                event.getUrl().getShortCode(),
                event.getUrl().getUserId(),
                event.getReferrer(),
                event.getCountry(),
                event.getUserAgent(),
                event.getOccurredAt()
        );
    }
}
