package com.gateway.api_gateway.filter;

import com.gateway.api_gateway.security.JwtUtil;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory <JwtAuthFilter.Config> {
    private final JwtUtil jwtUtil;

}
