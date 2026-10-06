///usr/bin/env jbang "$0" "$@" ; exit $?
//JAVA 17
//DEPS org.springframework.boot:spring-boot-starter-web:3.5.3
//DEPS org.togglz:togglz-spring-boot-starter:4.4.0
//DEPS org.togglz:togglz-console-spring-boot-starter:4.4.0

package me.abratuhi.demo.openfeature;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.togglz.core.Feature;
import org.togglz.core.annotation.EnabledByDefault;
import org.togglz.core.annotation.Label;
import org.togglz.core.context.FeatureContext;
import org.togglz.core.manager.EnumBasedFeatureProvider;
import org.togglz.core.manager.FeatureManager;
import org.togglz.core.manager.TogglzConfig;
import org.togglz.core.repository.StateRepository;
import org.togglz.core.repository.mem.InMemoryStateRepository;
import org.togglz.core.spi.FeatureProvider;
import org.togglz.core.user.FeatureUser;
import org.togglz.core.user.SimpleFeatureUser;
import org.togglz.core.user.UserProvider;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.management.MBeanServer;
import javax.management.ObjectName;

@SpringBootApplication
public class demo3 {

	public static void main(String[] args) {
		SpringApplication.run(demo3.class, args);
	}

	@SuppressWarnings("unchecked")
	@Bean
	public FeatureProvider featureProvider() {
		return new EnumBasedFeatureProvider(MyFeatures.class);
	}

	/* don't do that on PROD! */
	@Bean
	public UserProvider getUserProvider() {
		return new UserProvider() {
			@Override
			public FeatureUser getCurrentUser() {
				return new SimpleFeatureUser("admin", true);
			}
		};
	}

	public enum MyFeatures implements Feature {

		@Label("list-pizza-hawaii")
		FEATURE_LIST_PIZZA_HAWAII;

		public boolean isActive() {
			return FeatureContext.getFeatureManager().isActive(this);
		}
	}

	@RestController
	public class PizzaController {

		private static final String HAWAII = "Hawaii";

		private static final Set<Pizza> PIZZAS = Set.of(new Pizza("Margherita"), new Pizza("Marinara"),
				new Pizza("Capricciosa"),
				new Pizza("Quattro Formaggi"), new Pizza(HAWAII));
		@Autowired
		private FeatureManager featureManager;

		@GetMapping("/pizza")
		public ResponseEntity<Pizzas> getPizzas() {
			ArrayList<Pizza> pizzas = new ArrayList<>(PIZZAS);
			if (!featureManager.isActive(MyFeatures.FEATURE_LIST_PIZZA_HAWAII)) {
				pizzas.removeIf(pizza -> pizza.name().equals(HAWAII));
			}
			return ResponseEntity.ok(new Pizzas(pizzas));
		}

		@PostMapping("/pizza")
		public ResponseEntity<Pizza> orderPizza(@RequestBody Pizza pizza) {
			if (!PIZZAS.contains(pizza)) {
				return ResponseEntity.notFound().build();
			}
			if (pizza.name().equals(HAWAII)
					&& !featureManager.isActive(MyFeatures.FEATURE_LIST_PIZZA_HAWAII)) {
				return ResponseEntity.badRequest().body(new Pizza("Very bad taste!"));
			}
			return ResponseEntity.ok(pizza);
		}

		public record Pizzas(List<Pizza> pizzas) {
		}

		public record Pizza(String name) {
		}
	}
}
