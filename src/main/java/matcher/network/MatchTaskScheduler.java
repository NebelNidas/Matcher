package matcher.network;

import matcher.network.packet.s2c.MatchClassesTaskS2C;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class MatchTaskScheduler {
	public MatchTaskScheduler(NetworkHandler networkHandler, Supplier<ExecutorService> localExecutorSupplier) {
		this.networkHandler = networkHandler;
		this.localExecutor = localExecutorSupplier;
	}

	public void execute() {
		for (ConnectedLanPeer peer : networkHandler.getConnections().tcpPeersByAddress.values()) {
			if (peer.getAnnouncement().data().acceptsAnyTasks()) {
				peer.send(new MatchClassesTaskS2C(new MatchClassesTaskS2C.Data())
			}
		}
	}

	public <T, C> void runInParallel(List<T> workSet, Consumer<T> worker, DoubleConsumer progressReceiver) {
		if (workSet.isEmpty()) return;

		Collection<ConnectedLanPeer> peers = networkHandler.getConnections().tcpPeersByAddress.values();

		AtomicInteger itemsDone = new AtomicInteger();
		int updateRate = Math.max(1, workSet.size() / 200);

		try {
			List<Future<Void>> futures = matchingThreadPool.invokeAll(workSet.stream().<Callable<Void>>map(workItem -> () -> {
				worker.accept(workItem);

				int cItemsDone = itemsDone.incrementAndGet();

				if ((cItemsDone % updateRate) == 0) {
					progressReceiver.accept((double) cItemsDone / workSet.size());
				}

				return null;
			}).collect(Collectors.toList()));

			for (Future<Void> future : futures) {
				future.get();
			}
		} catch (ExecutionException | InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private final NetworkHandler networkHandler;
	private Supplier<ExecutorService> localExecutor;
}
