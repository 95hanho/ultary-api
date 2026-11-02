package me._hanho.ultary.domain.feed;

import java.util.Set;

/** 상품 링크태그 전용으로 운영하는 계정. 일반 사용자의 상품 후기는 광고로 분류하지 않는다. */
public final class FeedAdvertisementPolicy {
    private FeedAdvertisementPolicy() {}
    public static final Set<Long> SELLER_USER_NOS = Set.of(6129L, 6130L, 6131L, 6132L, 6133L);

    public static boolean isAdvertisement(Long author) {
        return author != null && SELLER_USER_NOS.contains(author);
    }
}
