package com.routerecipt.project.redis.bloomFilter;

import io.lettuce.core.protocol.ProtocolKeyword;

public enum BloomCommand implements ProtocolKeyword {
	ADD("CF.ADD"),EXISTS("CF.EXISTS");
	
	private final byte[] bytes;
	
	BloomCommand(String keyword) {
		this.bytes = keyword.getBytes();
	}
	
	@Override
	public byte[] getBytes() {
		return bytes;
	}
}
