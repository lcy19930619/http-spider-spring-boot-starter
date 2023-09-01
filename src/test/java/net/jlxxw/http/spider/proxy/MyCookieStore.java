package net.jlxxw.http.spider.proxy;

import java.util.Date;
import java.util.List;
import net.jlxxw.http.spider.base.AbstractCookieStore;
import org.apache.http.cookie.Cookie;
import org.springframework.stereotype.Component;

/**
 * @author chunyang.leng
 * @date 2023-09-01 15:24
 */
@Component
public class MyCookieStore extends AbstractCookieStore {
    /**
     * Adds an {@link Cookie}, replacing any existing equivalent cookies.
     * If the given cookie has already expired it will not be added, but existing
     * values will still be removed.
     *
     * @param cookie the {@link Cookie cookie} to be added
     */
    @Override public void addCookie(Cookie cookie) {

    }

    /**
     * Returns all cookies contained in this store.
     *
     * @return all cookies
     */
    @Override public List<Cookie> getCookies() {
        return null;
    }

    /**
     * Removes all of {@link Cookie}s in this store that have expired by
     * the specified {@link Date}.
     *
     * @param date
     * @return true if any cookies were purged.
     */
    @Override public boolean clearExpired(Date date) {
        return false;
    }

    /**
     * Clears all cookies.
     */
    @Override public void clear() {

    }
}
