package me.abratuhi.demo.openfeature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import dev.openfeature.sdk.Client;
import dev.openfeature.sdk.ImmutableContext;
import dev.openfeature.sdk.Value;

import io.quarkus.runtime.Quarkus;
import io.quarkus.runtime.annotations.QuarkusMain;

@QuarkusMain
public class demo8 {

	private static final String FEATURE_PIZZA_HAWAII = "list-pizza-hawaii";

	public static void main(String[] args) {
		Quarkus.run(args);
	}

	@Path("/pizza")
	@ApplicationScoped
	@jakarta.ws.rs.Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public static class PizzaController {

		private static final String HAWAII = "Hawaii";

		private static final Set<Pizza> PIZZAS = Set.of(new Pizza("Margherita"), new Pizza("Marinara"),
				new Pizza("Capricciosa"),
				new Pizza("Quattro Formaggi"), new Pizza(HAWAII));

		@Inject
		Client client;

		@GET
		public Response getPizzas(@HeaderParam("x-pizzeria-location") String location) {
			ArrayList<Pizza> pizzas = new ArrayList<>(PIZZAS);
			if (!client.getBooleanValue(FEATURE_PIZZA_HAWAII, Boolean.FALSE,
					new ImmutableContext(Map.of("location", new Value(location))))) {
				pizzas.removeIf(pizza -> pizza.name().equals(HAWAII));
			}
			return Response.ok(new Pizzas(pizzas)).build();
		}

		@POST
		public Response orderPizza(@HeaderParam("x-pizzeria-location") String location, Pizza pizza) {
			if (!PIZZAS.contains(pizza)) {
				return Response.status(Response.Status.NOT_FOUND).build();
			}
			if (pizza.name().equals(HAWAII) && !client.getBooleanValue(FEATURE_PIZZA_HAWAII, Boolean.FALSE,
					new ImmutableContext(Map.of("location", new Value(location))))) {
				return Response.status(Response.Status.BAD_REQUEST).entity(new Pizza("Very bad taste!")).build();
			}
			return Response.ok(pizza).build();
		}
	}

	public record Pizzas(List<Pizza> pizzas) {
	}

	public record Pizza(String name) {
	}
}
