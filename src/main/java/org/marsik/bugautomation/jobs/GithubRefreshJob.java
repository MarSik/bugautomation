package org.marsik.bugautomation.jobs;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;

import org.marsik.bugautomation.facts.Bug;
import org.marsik.bugautomation.facts.GithubIssue;
import org.marsik.bugautomation.github.Issue;
import org.marsik.bugautomation.github.User;
import org.marsik.bugautomation.services.BugMatchingService;
import org.marsik.bugautomation.services.ConfigurationService;
import org.marsik.bugautomation.services.FactService;
import org.marsik.bugautomation.services.RuleGlobalsService;
import org.marsik.bugautomation.services.UserMatchingService;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
public class GithubRefreshJob implements Runnable {
    private static final String GITHUB_API = "api.github.com";
    private static final String GITHUB_API_VERSION = "application/vnd.github.v3+json";

    @Getter
    private static final AtomicBoolean finished = new AtomicBoolean(false);

    @Inject
    FactService factService;

    @Inject
    ConfigurationService configurationService;

    @Inject
    UserMatchingService userMatchingService;

    @Inject
    BugMatchingService bugMatchingService;

    @Inject
    RuleGlobalsService ruleGlobalsService;

    OkHttpClient httpClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .build();

    // TODO unify the object mappers when RestEasy is replaced with OkHttp3
    Gson gson = new Gson();

    @Value
    private static class Repo {
        String owner;
        String repo;
    }

    @Override
    public void run() {
        final Optional<String> userAgent = configurationService.get(ConfigurationService.GITHUB_USER_AGENT);
        final Optional<String> watchRepos = configurationService.get(ConfigurationService.GITHUB_WATCH);

        if (!userAgent.isPresent() || !watchRepos.isPresent()) {
            log.error("GitHub not configured. Skipping.");
            return;
        }

        List<Repo> repos = Stream.of(watchRepos.get().split("[ ,]+"))
                .map(s -> s.split("/"))
                .map(ss -> new Repo(ss[0], ss[1]))
                .collect(Collectors.toList());

        for (Repo repo: repos) {
            HttpUrl.Builder url = new HttpUrl.Builder()
                    .scheme("https")
                    .host(GITHUB_API)
                    .addPathSegment("repos")
                    .addPathSegment(repo.getOwner())
                    .addPathSegment(repo.getRepo())
                    .addPathSegment("issues");

            Request.Builder request = new Request.Builder()
                    .url(url.build())
                    .header("User-Agent", userAgent.get())
                    .header("Accept", GITHUB_API_VERSION);

            final List<Issue> issues;

            log.info("Refreshing issues for github {}/{}", repo.getOwner(), repo.getRepo());

            try (Response response = httpClient.newCall(request.build()).execute()) {
                if (!response.isSuccessful()) {
                    log.error("Could not retrieve the list of issues from {}: {}", repo, response.toString());
                    continue;
                }

                String body = response.body().string();
                issues = gson.fromJson(body, new TypeToken<List<Issue>>(){}.getType());
            } catch (IOException e) {
                log.error("Could not retrieve the list of issues from {}", repo, e);
                continue;
            }

            log.info("Retrieved {} github issues for {}/{}",
                    issues.size(), repo.getOwner(), repo.getRepo());

            Map<String, GithubIssue> retrievedIssues = new HashMap<>();

            for (Issue issue: issues) {
                Bug bug = new Bug("github-" + issue.getId().toString());
                GithubIssue kiIssue = GithubIssue.builder()
                        .id(issue.getNumber())
                        .uid(bug.getId())
                        .bug(bug)
                        .title(issue.getTitle())
                        .description(issue.getBody() + "\n\n{{ bug:" + bug.getId() + " }}")
                        .githubUrl(issue.getHtml_url())
                        .repo(repo.getRepo())
                        .repoOwner(repo.getOwner())
                        .build();

                for (User u: issue.getAssignees()) {
                    userMatchingService.getByGithub(u.getLogin()).ifPresent(kiIssue.getAssignedTo()::add);
                }

                if (issue.getAssignee() != null) {
                    userMatchingService.getByGithub(issue.getAssignee().getLogin()).ifPresent(kiIssue.getAssignedTo()::add);
                }

                retrievedIssues.put(kiIssue.getUid(), kiIssue);
            }

            // Update fact database
            factService.addOrUpdateFacts(retrievedIssues.values());

            // Forget about bugs that were assigned out of scope
            Collection<GithubIssue> issuesToRemove = ruleGlobalsService.getGithubIssues();
            issuesToRemove = issuesToRemove.stream()
                    .filter(b -> !retrievedIssues.containsKey(b.getUid()))
                    .collect(Collectors.toList());

            log.info("Forgetting about issues: {}", issuesToRemove.stream()
                    .map(GithubIssue::getId).collect(Collectors.toList()));

            issuesToRemove.stream()
                    .forEach(factService::removeFact);
        }

        finished.set(true);
    }
}
