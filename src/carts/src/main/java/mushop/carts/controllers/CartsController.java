/*
 * Copyright 2021 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package mushop.carts.controllers;

import io.micrometer.core.annotation.Timed;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.http.hateoas.JsonError;
import io.micronaut.http.hateoas.Link;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import mushop.carts.entities.Cart;
import mushop.carts.entities.Item;
import mushop.carts.repositories.CartRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Controller("/carts")
@ExecuteOn(TaskExecutors.IO)
public class CartsController {

    public static final Logger LOG = LoggerFactory.getLogger(CartsController.class);

    private final CartRepository cartRepository;

    public CartsController(CartRepository cartRepository) {
        this.cartRepository = cartRepository;
    }

    @Get("/{cartId}")
    public Cart getCart(String cartId) {
        Cart cart = cartRepository.getById(cartId);
        if (cart == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND,
                    "Cart with id " + cartId + " not found");
        }
        return cart;
    }

    @Get("/{cartId}/items")
    public List<Item> getCartItems(String cartId) {
        Cart cart = cartRepository.getById(cartId);
        if (cart == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND,
                    "Cart with id " + cartId + " not found");
        }
        return cart.getItems();
    }

    @Delete("/{cartId}")
    public Cart deleteCart(String cartId) {
        Cart cart = cartRepository.getById(cartId);
        if (cart == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND,
                    "Cart with id " + cartId + " not found");
        }

        if (cartRepository.deleteCart(cartId)) {
            LOG.info("Cart deleted: {}", cart);
            return cart;
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND,
                    "Failed to delete cart " + cartId);
        }
    }

    @Delete("/{cartId}/items/{itemId}")
    @Timed("carts.updated.timer")
    public Cart deleteCartItem(String cartId, String itemId) {
        Cart cart = cartRepository.getById(cartId);
        if (cart == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND,
                    "Cart with id " + cartId + " not found");
        }

        if (!cart.removeItem(itemId)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND,
                    "Cart item with id " + itemId + " not found " + cart);
        }

        cartRepository.save(cart);
        LOG.info("Item deleted: {}", cart);
        return cart;
    }

    @Post("/{cartId}")
    public HttpResponse<Cart> postCart(@PathVariable String cartId,
                                       @Body Cart newCart) {

        Cart cart = cartRepository.getById(cartId);
        if (cart == null) {
            newCart.setId(cartId);
            cartRepository.save(newCart);
            LOG.info("Cart created: {}", newCart);
            return HttpResponse.created(newCart);
        } else {
            cart.merge(newCart);
            cartRepository.update(cart);
            LOG.info("Cart updated: {}", cart);
            return HttpResponse.ok(cart);
        }
    }

    @Put("/{cartId}/items")
    public Cart updateCartItem(@PathVariable String cartId, @Body Item qItem) {
        Cart cart = cartRepository.getById(cartId);
        if (cart == null) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND,
                    "Cart with id " + cartId + " not found");
        }

        for (Item item : cart.getItems()) {
            if (item.getItemId().equals(qItem.getItemId())) {
                item.setQuantity(qItem.getQuantity());

                cartRepository.save(cart);
                LOG.info("Cart item updated: {}", cart);
                return cart;
            }
        }
        return null;
    }

    @Error
    HttpResponse<JsonError> failedOperation(HttpRequest<?> request, Exception exception) {
        JsonError error = new JsonError("Failed operation: " + exception.getMessage())
                .link(Link.SELF, Link.of(request.getUri()));

        return HttpResponse.<JsonError>status(HttpStatus.BAD_REQUEST, "Bad request")
                .body(error);
    }
}
