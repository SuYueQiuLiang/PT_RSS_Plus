public class WebSite {
    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public WebSite(String url, String cookie ,String host) {
        this.url = url;
        this.cookie = cookie;
        this.host = host;
    }

    private String url,cookie,host;

}
