package matcher.bcprovider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SharedBcPrimitiveTypes {
	private static BcPrimitiveType register(String name) {
		BcPrimitiveType newType = new BcPrimitiveType() {
			@Override
			public int getIndex() {
				return indexGenerator;
			}

			@Override
			public String getName() {
				return name;
			}
		};

		all.add(newType);
		indexTypeMap.put(indexGenerator, newType);
		indexGenerator++;
		return newType;
	}

	public static List<BcPrimitiveType> getAll() {
		return Collections.unmodifiableList(all);
	}

	public static BcPrimitiveType getByIndex(int index) {
		return indexTypeMap.get(index);
	}

	public static final BcPrimitiveType BOOLEAN = register("BOOLEAN");
	public static final BcPrimitiveType CHAR = register("CHAR");
	public static final BcPrimitiveType FLOAT = register("FLOAT");
	public static final BcPrimitiveType DOUBLE = register("DOUBLE");
	public static final BcPrimitiveType BYTE = register("BYTE");
	public static final BcPrimitiveType SHORT = register("SHORT");
	public static final BcPrimitiveType INT = register("INT");
	public static final BcPrimitiveType LONG = register("LONG");

	private static int indexGenerator = 4;
	private static final List<BcPrimitiveType> all = new ArrayList<>();
	private static final Map<Integer, BcPrimitiveType> indexTypeMap = new HashMap<>();
}
