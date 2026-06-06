import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class github_activity {

    private static final String BASE_URL = "https://api.github.com/users/";
    private static final String EVENTS_PATH = "/events/public";

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Empty arguments.");
            return;
        }

        String username = args[0];
        String url = getUrlForUsername(username);

        try {
            HttpClient client = buildHttpClient();
            HttpRequest request = buildHttpRequest(url);

            HttpResponse<String> response = getResponse(client, request).orElseThrow();

            int status = response.statusCode();

            if (status == 404) {
                System.err.println("User Not Found");
                return;
            }

            if (status == 403) {
                System.err.println("Rate Limit Hit");
                return;
            }

            if (status == 401) {
                System.err.println("Authentication Problem Occurred.");
                return;
            }

            if (status == 500) {
                System.err.println("Server is on FIRE");
                return;
            }

            printRecentActivities(response.body());

        } catch (NoSuchElementException e) {
            System.out.println("Error to get response: " + e.getMessage());
        }

    }

    private static String getUrlForUsername(String username) {
        return BASE_URL + username + EVENTS_PATH;
    }

    private static HttpClient buildHttpClient() {
        return HttpClient.newHttpClient();
    }

    private static HttpRequest buildHttpRequest(String url) {
        return HttpRequest
                .newBuilder()
                .uri(URI.create(url))
                .header("Jerme", "com-github-getuser")
                .header("Accept", "application/vnd.github+json")
                .GET()
                .build();
    }

    private static Optional<HttpResponse<String>> getResponse(HttpClient client, HttpRequest request)  {
        try {
            return Optional.of(client.send(request, HttpResponse.BodyHandlers.ofString()));
        } catch (InterruptedException e) {
            System.out.println("Session Interrupted: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("Connection Error: " + e.getMessage());
        }

        return Optional.empty();
    }

    private static void printRecentActivities(String responseBody) {
        Pattern typePattern = Pattern.compile("\"type\":\"(.*?)\"");
		Pattern repoPattern = Pattern.compile("\"repo\":\\{\"id\":\\d+,\"name\":\"(.*?)\"");
		Pattern datePattern = Pattern.compile("\"created_at\":\"(.*?)\"");
	
		Matcher typeMatcher = typePattern.matcher(responseBody);
		Matcher repoMatcher = repoPattern.matcher(responseBody);
		Matcher dateMatcher = datePattern.matcher(responseBody);

		while (typeMatcher.find() && repoMatcher.find() && dateMatcher.find()) {
    		String type = typeMatcher.group(1);
    		String repo = repoMatcher.group(1);
    		String date = dateMatcher.group(1).replace("T", " ").replace("Z", "");

    		System.out.println(type + " on " + repo + " at " + date);
			
		}
    }
}
