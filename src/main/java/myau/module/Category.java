package myau.module;

public enum Category {
	COMBAT("Combat"),
	MOVEMENT("Movement"),
	RENDER("Render"),
	PLAYER("Player"),
	MISC("Misc");
	
	final String name;

	private Category(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
