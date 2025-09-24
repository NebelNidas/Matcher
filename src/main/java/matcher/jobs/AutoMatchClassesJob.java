package matcher.jobs;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import job4j.JobState;

import matcher.Matcher;
import matcher.Util;
import matcher.classifier.ClassClassifier;
import matcher.classifier.ClassifierLevel;
import matcher.classifier.RankResult;
import matcher.network.ConnectedLanPeer;
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
		Collection<ConnectedLanPeer> peers = networkHandler.getConnections().tcpPeersByAddress.values();
		List<ClassInstance> classesToMatchLocally;
		List<List<ClassInstance>> classSetsToMatchRemotely;
		AutoMatchClassesLocalJob localJob;
		List<AutoMatchClassesRemoteJob> remoteJobs;

		if (classes.size() < 200 || peers.isEmpty()) {
			classesToMatchLocally = classes;
			classSetsToMatchRemotely = List.of();
		} else {
			List<List<ClassInstance>> classesPartitioned = Util.partition(classes, 1 + peers.size());
			classesToMatchLocally = classesPartitioned.get(0);
			classSetsToMatchRemotely = classesPartitioned.subList(1, classesPartitioned.size());
		}

		localJob = new AutoMatchClassesLocalJob(matcher, level, classesToMatchLocally);
		addSubJob(localJob, true);

		if (!classSetsToMatchRemotely.isEmpty()) {
			int i = 0;
			Iterator<ConnectedLanPeer> peerIt = peers.iterator();

			while (peerIt.hasNext()) {
				ConnectedLanPeer peer = peerIt.next();
				List<ClassInstance> classSet = classSetsToMatchRemotely.get(i);

				AutoMatchClassesRemoteJob remoteJob = new AutoMatchClassesRemoteJob(matcher, networkHandler, peer, level, classSet);
				addSubJob(remoteJob, true);

				i++;

				if (i >= classSetsToMatchRemotely.size()) {
					break;
				}
			}
		}

		matches.putAll(localJob.runAndAwait().getResult().orElseThrow());

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
