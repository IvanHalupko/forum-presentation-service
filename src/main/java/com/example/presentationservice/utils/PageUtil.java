package com.example.presentationservice.utils;

import static com.example.presentationservice.utils.Constants.LIMIT;

public class PageUtil {

    public static int getPages(long count) {
        int pages = (int) (count / LIMIT);
        if (count % LIMIT > 0)
            pages++;
        if (count == 0)
            pages = 0;
        return pages;
    }

    public static int pageValidation(int page, long count) {
        if (page < 0)
            return 0;
        if (page > (getPages(count) -1))
            return getPages(count) - 1;
        else
            return page;
    }
}
