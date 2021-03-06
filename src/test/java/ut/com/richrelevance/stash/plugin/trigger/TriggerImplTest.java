package ut.com.richrelevance.stash.plugin.trigger;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.InOrder;

import com.atlassian.stash.comment.Comment;
import com.atlassian.stash.event.pull.PullRequestCommentAddedEvent;
import com.atlassian.stash.event.pull.PullRequestEvent;
import com.atlassian.stash.pull.PullRequest;
import com.atlassian.stash.pull.PullRequestRef;
import com.atlassian.stash.repository.Repository;
import com.google.common.collect.Lists;
import com.richrelevance.stash.plugin.trigger.BranchPredicate;
import com.richrelevance.stash.plugin.trigger.BuildTriggerer;
import com.richrelevance.stash.plugin.trigger.Trigger;
import com.richrelevance.stash.plugin.trigger.TriggerImpl;
import com.richrelevance.stash.plugin.settings.BranchSettings;
import com.richrelevance.stash.plugin.settings.ImmutableBranchSettings;
import com.richrelevance.stash.plugin.settings.ImmutablePullRequestTriggerSettings;
import com.richrelevance.stash.plugin.settings.PullRequestTriggerSettings;
import com.richrelevance.stash.plugin.settings.PullRequestTriggerSettingsService;

/**
 * Created by dsobral on 1/31/14.
 */
public class TriggerImplTest {

  private static final String url = "fakeUrl";
  private static final String user = "fake user";
  private static final String password = "fake password";

  private static final PullRequestTriggerSettings settingsEnabled = new ImmutablePullRequestTriggerSettings(true, url,
    user, password);
  private static final PullRequestTriggerSettings settingsRegexEnabled = new ImmutablePullRequestTriggerSettings(true, url,
    user, password);

  private static final PullRequestTriggerSettings settingsDisabled = new ImmutablePullRequestTriggerSettings(false, url,
    user, password);

  private static final String branchName = "default branch";
  private static final String wildcardBranchName = ".*";
  private static final String unusedBranchName = "Unused";
  private static final String planName = "StandardPlan";
  private static final String retestMsg = "Retest this please";
  private static final String retestRegex = "(?i)retest this,? please|klaatu barada nikto";
  private static final String alternateMsg = "Alternate Message";

  private static final ImmutableBranchSettings immutableBranchSettings =
    new ImmutableBranchSettings(true, branchName, planName, retestRegex);

  private static final ImmutableBranchSettings wildcardBranchSettings =
    new ImmutableBranchSettings(true, wildcardBranchName, "somethingElse", retestRegex);

  private static final ImmutableBranchSettings alternateBranchSettings =
    new ImmutableBranchSettings(true, branchName, "alternatePlan", alternateMsg);

  private static final ImmutableBranchSettings unusedBranchSettings =
    new ImmutableBranchSettings(true, unusedBranchName, planName, alternateMsg);

  private static final ImmutableBranchSettings emptyMsgBranchSettings =
    new ImmutableBranchSettings(true, branchName, planName, "");

  private static final ImmutableBranchSettings onDemandOnlyBranchSettings =
    new ImmutableBranchSettings(false, branchName, planName, retestRegex);

  private static final PullRequestTriggerSettingsService settingsServiceEnabled =
    new SettingsService(settingsEnabled, immutableBranchSettings);

  private static final PullRequestTriggerSettingsService settingsServiceRegexEnabled =
    new SettingsService(settingsRegexEnabled, immutableBranchSettings);

  private static final PullRequestTriggerSettingsService settingsServiceEmptyMsg =
    new SettingsService(settingsEnabled, emptyMsgBranchSettings);

  private static final PullRequestTriggerSettingsService settingsServiceDisabled =
    new SettingsService(settingsDisabled, immutableBranchSettings);

  private static final PullRequestTriggerSettingsService settingsServiceOnDemand =
    new SettingsService(settingsEnabled, onDemandOnlyBranchSettings);

  private static final PullRequestTriggerSettingsService settingsServiceEnabledMultiBranch =
    new SettingsService(settingsEnabled, immutableBranchSettings, wildcardBranchSettings, alternateBranchSettings, unusedBranchSettings);

  @Test
  public void automaticTriggerBuildIfBranchAutomaticBuildIsEnabledTest() {
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);
    PullRequestEvent event = mock(PullRequestEvent.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);

    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref, ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(branchName);
    when(pullRequest.getId()).thenReturn(1L);

    Trigger trigger = new TriggerImpl(settingsServiceEnabled, buildTriggerer);

    trigger.automaticTrigger(event);

    verify(buildTriggerer).invoke(1L, settingsEnabled, immutableBranchSettings);
  }

  @Test
  public void automaticTriggerDoesNotBuildIfBranchAutomaticBuildIsDisabledTest() {
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);
    PullRequestEvent event = mock(PullRequestEvent.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);

    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref, ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(branchName);
    when(pullRequest.getId()).thenReturn(1L);

    Trigger trigger = new TriggerImpl(settingsServiceOnDemand, buildTriggerer);

    trigger.automaticTrigger(event);

    verify(buildTriggerer, never()).invoke(anyLong(), any(PullRequestTriggerSettings.class), any(BranchSettings.class));
  }

  @Test
  public void onDemandTriggerBuildsIfMessageMatchesSettingsTest() {
    PullRequestCommentAddedEvent event = mock(PullRequestCommentAddedEvent.class);
    Comment comment = mock(Comment.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);

    when(event.getComment()).thenReturn(comment);
    when(comment.getText()).thenReturn(retestMsg);
    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(branchName);
    when(pullRequest.getId()).thenReturn(1L);

    Trigger trigger = new TriggerImpl(settingsServiceEnabled, buildTriggerer);

    trigger.onDemandTrigger(event);

    verify(buildTriggerer).invoke(1L, settingsEnabled, immutableBranchSettings);
  }

  @Test
  public void onDemandTriggerBuildsIfMessageMatchesRegexTest() {
    PullRequestCommentAddedEvent event = mock(PullRequestCommentAddedEvent.class);
    Comment comment = mock(Comment.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);

    when(event.getComment()).thenReturn(comment);
    when(comment.getText()).thenReturn("KLAATU BARADA NIKTO");
    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(branchName);
    when(pullRequest.getId()).thenReturn(1L);

    Trigger trigger = new TriggerImpl(settingsServiceRegexEnabled, buildTriggerer);

    trigger.onDemandTrigger(event);

    verify(buildTriggerer).invoke(1L, settingsRegexEnabled, immutableBranchSettings);
  }

  @Test
  public void onDemandTriggersOnlyMatchingBranchesWithMatchingMessagesTest() {
    PullRequestCommentAddedEvent event = mock(PullRequestCommentAddedEvent.class);
    Comment comment = mock(Comment.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);

    when(event.getComment()).thenReturn(comment);
    when(comment.getText()).thenReturn(alternateMsg);

    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(branchName);
    when(pullRequest.getId()).thenReturn(1L);

    Trigger trigger = new TriggerImpl(settingsServiceEnabledMultiBranch, buildTriggerer);

    trigger.onDemandTrigger(event);

    InOrder inOrder = inOrder(buildTriggerer);
    inOrder.verify(buildTriggerer).invoke(1L, settingsEnabled, alternateBranchSettings);
    inOrder.verifyNoMoreInteractions();
  }

  @Test
  public void onDemandDoesNotTriggerBuildsIfMessageDoesNotMatchSettingsTest() {
    PullRequestCommentAddedEvent event = mock(PullRequestCommentAddedEvent.class);
    Comment comment = mock(Comment.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);

    when(event.getComment()).thenReturn(comment);
    when(comment.getText()).thenReturn("Do Not Retest");
    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(branchName);
    when(pullRequest.getId()).thenReturn(1L);

    Trigger trigger = new TriggerImpl(settingsServiceEnabled, buildTriggerer);

    trigger.onDemandTrigger(event);

    verify(buildTriggerer, never()).invoke(anyLong(), any(PullRequestTriggerSettings.class), any(BranchSettings.class));
  }

  @Test
  public void onDemandDoesNotTriggerBuildsIfMessageIsEmptyTest() {
    PullRequestCommentAddedEvent event = mock(PullRequestCommentAddedEvent.class);
    Comment comment = mock(Comment.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);

    when(event.getComment()).thenReturn(comment);
    when(comment.getText()).thenReturn(retestMsg);
    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(branchName);
    when(pullRequest.getId()).thenReturn(1L);

    Trigger trigger = new TriggerImpl(settingsServiceEmptyMsg, buildTriggerer);

    trigger.onDemandTrigger(event);

    verify(buildTriggerer, never()).invoke(anyLong(), any(PullRequestTriggerSettings.class), any(BranchSettings.class));
  }

  @Test
  public void triggerBuildDoesNotTriggerBuildsIfRepositoryDisabledTest() {
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);
    PullRequestEvent event = mock(PullRequestEvent.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);

    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref, ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(branchName);

    Trigger trigger = new TriggerImpl(settingsServiceDisabled, buildTriggerer);

    trigger.triggerBuild(event, AlwaysTruePredicate.instance);

    verify(buildTriggerer, never()).invoke(anyLong(), any(PullRequestTriggerSettings.class), any(BranchSettings.class));
  }

  @Test
  public void triggerBuildDoesNotTriggerBuildsIfBranchSettingsIsNullTest() {
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);
    PullRequestEvent event = mock(PullRequestEvent.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);

    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref, ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn("another branch");

    Trigger trigger = new TriggerImpl(settingsServiceEnabled, buildTriggerer);

    trigger.triggerBuild(event, AlwaysTruePredicate.instance);

    verify(buildTriggerer, never()).invoke(anyLong(), any(PullRequestTriggerSettings.class), any(BranchSettings.class));
  }

  @Test
  public void triggerBuildBuildsIfEnabledAndBranchSettingsExistAndAutomaticBuildIsEnabledTest() {
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);
    PullRequestEvent event = mock(PullRequestEvent.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);

    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref, ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(branchName);
    when(pullRequest.getId()).thenReturn(1L);

    Trigger trigger = new TriggerImpl(settingsServiceEnabled, buildTriggerer);

    trigger.triggerBuild(event, AlwaysTruePredicate.instance);

    verify(buildTriggerer).invoke(1L, settingsEnabled, immutableBranchSettings);
  }

  @Test
  public void triggerBuildTriggersMultipleBuildsIfMoreThanOneBranchMatchesNameTest() {
    BuildTriggerer buildTriggerer = mock(BuildTriggerer.class);
    PullRequestEvent event = mock(PullRequestEvent.class);
    PullRequest pullRequest = mock(PullRequest.class);
    PullRequestRef ref = mock(PullRequestRef.class);
    Repository repository = mock(Repository.class);

    when(event.getPullRequest()).thenReturn(pullRequest);
    when(pullRequest.getToRef()).thenReturn(ref, ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(branchName);
    when(pullRequest.getId()).thenReturn(1L);

    Trigger trigger = new TriggerImpl(settingsServiceEnabledMultiBranch, buildTriggerer);

    trigger.triggerBuild(event, AlwaysTruePredicate.instance);

    verify(buildTriggerer).invoke(1L, settingsEnabled, immutableBranchSettings);
    verify(buildTriggerer).invoke(1L, settingsEnabled, wildcardBranchSettings);
  }

  private static class SettingsService implements PullRequestTriggerSettingsService {
    private final PullRequestTriggerSettings settings;
    private final List<BranchSettings> branchSettings;

    public SettingsService(PullRequestTriggerSettings settings, BranchSettings... branchSettings) {
      this.settings = settings;
      this.branchSettings = Lists.newArrayList(branchSettings);
    }

    @Override
    public PullRequestTriggerSettings getPullRequestTriggerSettings(Repository repository) {
      return settings;
    }

    @Override
    public PullRequestTriggerSettings setPullRequestTriggerSettings(Repository repository, PullRequestTriggerSettings settings) {
      return null;
    }

    @Override
    public List<BranchSettings> getBranchSettings(Repository repository) {
      return new ArrayList<BranchSettings>(branchSettings);
    }

    @Override
    public void setBranch(Repository repository, String branchName, BranchSettings settings) {

    }

    @Override
    public void deleteBranch(Repository repository, String branchName) {

    }

    @Override
    public List<BranchSettings> getBranchSettingsForBranch(Repository repository, String branchPattern) {
      List<BranchSettings> result = new ArrayList<BranchSettings>();
      for (BranchSettings branchSetting : branchSettings) {
        if (branchPattern.matches(branchSetting.getName()))
          result.add(branchSetting);
      }
      return result;
    }
  }

  private static class AlwaysTruePredicate implements BranchPredicate {
    public static final AlwaysTruePredicate instance = new AlwaysTruePredicate();

    private AlwaysTruePredicate() {
    }

    @Override
    public boolean matches(BranchSettings branchSettings) {
      return true;
    }
  }
}
