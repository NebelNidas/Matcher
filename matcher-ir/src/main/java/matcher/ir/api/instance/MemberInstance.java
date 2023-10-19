package matcher.ir.api.instance;

import java.util.Comparator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import matcher.ir.api.access.PotentiallyFinal;
import matcher.ir.api.access.PotentiallyMandated;
import matcher.ir.api.access.PotentiallyPrivate;
import matcher.ir.api.access.PotentiallyProtected;
import matcher.ir.api.access.PotentiallyPublic;
import matcher.ir.api.access.PotentiallySynthetic;
import matcher.ir.api.instance.info.MemberNameInfo;

public sealed interface MemberInstance
		extends Instance,
		DescriptorOwner,
		PotentiallyPublic,
		PotentiallyPrivate,
		PotentiallyProtected,
		PotentiallyFinal,
		PotentiallySynthetic,
		PotentiallyMandated,
		Comparable<MemberInstance>
		permits FieldInstance, MethodInstance {
	@Override
	ClassInstance getOwner();

	@Override
	@NotNull
	MemberNameInfo getNameInfo();

	/**
	 * @return The member's index in the list of the parent class's members.
	 * Fields are ordered before methods.
	 */
	@Range(from = -1, to = Integer.MAX_VALUE)
	int getPosition();

	/**
	 * @return Whether or not this member might be a record component. A return value
	 * of {@code true} doesn't guarantee that it actually is one. You still have to check
	 * that {@link #getLinkedRecordComponent()} isn't null.
	 */
	boolean canBeRecordComponent();

	/**
	 * @return The corresponding {@link MethodInstance} if called on a {@link FieldInstance},
	 * or the other way around. Null if {@link #canBeRecordComponent()} is {@code false}, or no
	 * linked component exists.
	 */
	@Nullable
	MemberInstance getLinkedRecordComponent();

	@Override
	default int compareTo(MemberInstance other) {
		return Comparator.comparingInt(MemberInstance::getPosition)
				.thenComparing((member) -> member.getNameInfo().getName())
				.thenComparing((member) -> member.getNameInfo().getDescriptor())
				.compare(this, other);
	}
}
