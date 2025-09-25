package matcher.jobs;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.DoubleConsumer;

import job4j.BuiltinJobCancellationReasons;
import job4j.JobState;

import matcher.Matcher;
import matcher.Util;
import matcher.classifier.ClassifierLevel;
import matcher.network.ConnectedLanPeer;
import matcher.network.NetworkHandler;
import matcher.network.packet.s2c.MatchClassesS2C;
import matcher.type.ClassInstance;

public class AutoMatchClassesRemoteJob extends MatcherJob<Map<ClassInstance, ClassInstance>> {
	public AutoMatchClassesRemoteJob(Matcher matcher, NetworkHandler networkHandler, ConnectedLanPeer peer, ClassifierLevel level, List<ClassInstance> classes) {
		super(JobCategories.AUTOMATCH_CLASSES_REMOTE);

		this.matcher = matcher;
		this.networkHandler = networkHandler;
		this.peer = peer;
		this.level = level;
		this.classes = classes;
		this.matches = new ConcurrentHashMap<>(classes.size());
	}

	@Override
	protected Map<ClassInstance, ClassInstance> execute(DoubleConsumer progressReceiver) {
		List<List<ClassInstance>> classesPartitioned = Util.partitionIntoListsOfNSize(classes, 200);
		int chunksCount = classesPartitioned.size();
		addCancelListener(() -> {
			synchronized (monitor) {
				monitor.notifyAll();
			}
		});

		while (getState() != JobState.CANCELING && !classesPartitioned.isEmpty()) {
			List<ClassInstance> classesChunk = classesPartitioned.remove(0);

			networkHandler.getMatchedClassesC2SHandler().getJobsByPeer().put(peer, this);
			peer.send(new MatchClassesS2C(new MatchClassesS2C.Data(classesChunk.stream().map(ClassInstance::getId).toList(), level)));

			synchronized (monitor) {
				try {
					monitor.wait();
				} catch (InterruptedException e) {
					cancel(BuiltinJobCancellationReasons.INTERRUPTED);
					Thread.currentThread().interrupt();
					break;
				}
			}

			progressReceiver.accept((double) (chunksCount - classesPartitioned.size()) / chunksCount);
		}

		networkHandler.getMatchedClassesC2SHandler().getJobsByPeer().remove(peer);
		return matches;
	}

	public void addMatches(Map<String, String> matches) {
		if (state.isFinished()) {
			return;
		}

		for (Map.Entry<String, String> entry : matches.entrySet()) {
			String classAId = entry.getKey();
			String classBId = entry.getValue();
			ClassInstance classA = matcher.getEnv().getClsByIdA(classAId);
			ClassInstance classB = matcher.getEnv().getClsByIdB(classBId);

			Objects.requireNonNull(classA, "Class A not found: " + classAId);
			Objects.requireNonNull(classB, "Class B not found: " + classBId);

			this.matches.put(classA, classB);
		}

		synchronized (monitor) {
			monitor.notifyAll();
		}
	}

	private final Matcher matcher;
	private final NetworkHandler networkHandler;
	private final ConnectedLanPeer peer;
	private final ClassifierLevel level;
	private final List<ClassInstance> classes;
	private final Map<ClassInstance, ClassInstance> matches;
	private final Object monitor = new Object();
}
