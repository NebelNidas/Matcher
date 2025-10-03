package matcher.jobs;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;

import job4j.Job;

import matcher.Matcher;
import matcher.Util;
import matcher.classifier.ClassifierLevel;
import matcher.network.peer.Peer;
import matcher.network.NetworkHandler;
import matcher.type.ClassEnvironment;
import matcher.type.ClassInstance;

public class AutoMatchClassesJob extends MatcherJob<Boolean> {
	public AutoMatchClassesJob(Matcher matcher, NetworkHandler networkHandler, ClassifierLevel level) {
		super(JobCategories.AUTOMATCH_CLASSES);

		this.matcher = matcher;
		this.networkHandler = networkHandler;
		this.level = level;
	}

	@Override
	protected Boolean execute(DoubleConsumer progressReceiver) {
		ClassEnvironment env = matcher.getEnv();
		boolean assumeBothOrNoneObfuscated = env.assumeBothOrNoneObfuscated;
		Predicate<ClassInstance> filter = cls -> cls.isReal() && (!assumeBothOrNoneObfuscated || cls.isNameObfuscated()) && !cls.hasMatch() && cls.isMatchable();

		List<ClassInstance> classes = env.getClassesA().stream()
				.filter(filter)
				.toList();

		Map<ClassInstance, ClassInstance> matches = new ConcurrentHashMap<>(classes.size());
		List<Peer> peers = new ArrayList<>(networkHandler.getConnections().peersById.values());
		List<ClassInstance> classesToMatchLocally;
		List<List<ClassInstance>> classSetsToMatchRemotely;
		AutoMatchClassesLocalJob localJob;
		List<AutoMatchClassesRemoteJob> remoteJobs = new ArrayList<>();

		if (classes.size() < 200 || peers.isEmpty()) {
			classesToMatchLocally = classes;
			classSetsToMatchRemotely = List.of();
		} else {
			List<List<ClassInstance>> classesPartitioned = Util.partitionIntoNLists(classes, 1 + peers.size());
			classesToMatchLocally = classesPartitioned.get(0);
			classSetsToMatchRemotely = classesPartitioned.subList(1, classesPartitioned.size());
		}

		localJob = new AutoMatchClassesLocalJob(matcher, level, classesToMatchLocally);
		addSubJob(localJob, true);
		localJob.addFinishListener((result, error) -> matches.putAll(result.orElseThrow()));
		localJob.runAsync();

		if (!classSetsToMatchRemotely.isEmpty()) {
			for (int i = 0; i < classSetsToMatchRemotely.size(); i++) {
				Peer peer = peers.get(i);
				List<ClassInstance> classSet = classSetsToMatchRemotely.get(i);

				var remoteJob = new AutoMatchClassesRemoteJob(matcher, networkHandler, peer, level, classSet);
				addSubJob(remoteJob, true);
				remoteJobs.add(remoteJob);
				remoteJob.addFinishListener((result, error) -> matches.putAll(result.orElseThrow()));
				remoteJob.runAsync();
			}
		}

		localJob.await();
		remoteJobs.forEach(Job::await);

		Matcher.sanitizeMatches(matches);

		for (Map.Entry<ClassInstance, ClassInstance> entry : matches.entrySet()) {
			matcher.match(entry.getKey(), entry.getValue());
		}

		Matcher.LOGGER.info("Auto matched {} classes ({} unmatched, {} total)", matches.size(), (classes.size() - matches.size()), env.getClassesA().size());

		return !matches.isEmpty();
	}

	private final Matcher matcher;
	private final NetworkHandler networkHandler;
	private final ClassifierLevel level;
}
