package com.ferreusveritas.unifine;

public enum ShaderState {
	NULL(false),//No Optifine installed
	OFF(false),
	INTERNAL(true),
	SHADER(true);
	
	private boolean doesShading;
	
	private ShaderState(boolean doesShading) {
		this.doesShading = doesShading;
	}
	
	public boolean doesShading() {
		return doesShading;
	}
}