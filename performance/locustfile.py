import random
import time
from datetime import datetime
from locust import HttpUser, task, between, SequentialTaskSet

class CheckoutFlow(SequentialTaskSet):
    """Flujo de compra usando las rutas directas de microservicios a través del API Gateway"""
    
    def on_start(self):
        """Inicializa las variables para el flujo de checkout."""
        self.cart_id = None
        self.order_id = None
        self.payment_id = None
        self.order_fee = round(random.uniform(50.0, 1000.0), 2)
        self.user_id = self.user.user_id

    @task
    def add_to_cart(self):
        """Añadir productos al carrito usando order-service directo"""
        cart_data = {
            "userId": self.user_id,
            "isActive": True
        }
        
        with self.client.post(
            "/order-service/api/carts",
            json=cart_data,
            name="Create Cart (Order Service)",
            catch_response=True
        ) as response:
            if response.status_code in [200, 201]:
                try:
                    resp_json = response.json()
                    if isinstance(resp_json, dict):
                        if 'collection' in resp_json and resp_json['collection']:
                            self.cart_id = resp_json['collection'][0].get("cartId")
                        else:
                            self.cart_id = resp_json.get("cartId")
                    response.success()
                except Exception as e:
                    response.failure(f"Invalid response format: {e}")
                    self.interrupt()
            else:
                response.failure(f"Failed to create cart: {response.status_code}")
                self.interrupt()

    @task
    def create_order(self):
        """Crear una orden usando order-service"""
        if not self.cart_id:
            self.interrupt()
            return
        
        order_data = {
            "orderDesc": f"Order from user {self.user_id}",
            "orderFee": self.order_fee,
            "cartDto": {
                "cartId": self.cart_id
            }
        }

        with self.client.post(
            "/order-service/api/orders",
            json=order_data,
            name="Create Order",
            catch_response=True
        ) as response:
            if response.status_code in [200, 201]:
                try:
                    resp_json = response.json()
                    if isinstance(resp_json, dict):
                        if 'collection' in resp_json and resp_json['collection']:
                            self.order_id = resp_json['collection'][0].get("orderId")
                        else:
                            self.order_id = resp_json.get("orderId")
                    response.success()
                except Exception as e:
                    response.failure(f"Invalid response format: {e}")
                    self.interrupt()
            else:
                response.failure(f"Failed to create order: {response.status_code}")
                self.interrupt()

    @task
    def process_payment(self):
        """Procesar el pago usando payment-service"""
        if not self.order_id:
            self.interrupt()
            return
        
        payment_data = {
            "isPayed": False,
            "orderDto": {
                "orderId": self.order_id
            }
        }

        with self.client.post(
            "/payment-service/api/payments",
            json=payment_data,
            name="Process Payment",
            catch_response=True
        ) as response:
            if response.status_code in [200, 201]:
                response.success()
            else:
                response.failure(f"Failed to process payment: {response.status_code}")
        
        self.interrupt()


class EcommerceUser(HttpUser):
    wait_time = between(2, 6)
    user_id = None

    def on_start(self):
        """Inicializa un user_id aleatorio para el usuario"""
        self.user_id = random.randint(1, 100)

    @task(10)
    def browse_products_catalog(self):
        """Navegar por productos usando product-service directamente"""
        self.client.get("/product-service/api/products", name="Get All Products")
        
        product_id = random.randint(1, 100)
        self.client.get(f"/product-service/api/products/{product_id}", name="Get Product Details")

    @task(8)
    def browse_categories(self):
        """Navegar por categorías usando product-service"""
        self.client.get("/product-service/api/categories", name="Get All Categories")
        
        category_id = random.randint(1, 20)
        self.client.get(f"/product-service/api/categories/{category_id}", name="Get Category Details")

    @task(6)
    def browse_users(self):
        """Navegar por usuarios usando user-service"""
        self.client.get("/user-service/api/users", name="Get All Users")
        
        user_id = random.randint(1, 100)
        self.client.get(f"/user-service/api/users/{user_id}", name="Get User Details")

    @task(6)
    def browse_orders(self):
        """Navegar por órdenes usando order-service"""
        self.client.get("/order-service/api/orders", name="Get All Orders")
        
        self.client.get("/order-service/api/carts", name="Get All Carts")
        
        order_id = random.randint(1, 100)
        self.client.get(f"/order-service/api/orders/{order_id}", name="Get Order Details")

    @task(4)
    def browse_payments(self):
        """Navegar por pagos usando payment-service"""
        self.client.get("/payment-service/api/payments", name="Get All Payments")
        
        payment_id = random.randint(1, 100)
        self.client.get(f"/payment-service/api/payments/{payment_id}", name="Get Payment Details")

    @task(3)
    def manage_favourites(self):
        """Gestionar favoritos usando favourite-service"""
        self.client.get("/favourite-service/api/favourites", name="Get All Favourites")

        favourite_data = {
            "userId": self.user_id,
            "productId": random.randint(1, 100),
            "likeDate": datetime.now().strftime("%d-MM-yyyy__HH:mm:ss:SSSSSS")
        }
        
        self.client.post("/favourite-service/api/favourites", json=favourite_data, name="Add to Favourites")

    @task(2)
    def browse_shipping(self):
        """Navegar por order items usando shipping-service"""
        self.client.get("/shipping-service/api/shippings", name="Get All Order Items")
        
        order_id = random.randint(1, 50)
        product_id = random.randint(1, 50)
        self.client.get(f"/shipping-service/api/shippings/{order_id}/{product_id}", name="Get Order Item Details")

    @task(1)
    def complete_checkout(self):
        """Ejecutar flujo completo de checkout"""
        self.schedule_task(CheckoutFlow)

    @task(2)
    def health_checks(self):
        """Verificar salud de los servicios"""
        services = ["product-service", "user-service", "order-service", "payment-service", "favourite-service", "shipping-service"]
        service = random.choice(services)
        
        health_url = f"/{service}/actuator/health"
        main_url = f"/{service}/api"
        
        response = self.client.get(health_url, name=f"Health Check - {service}", catch_response=True)
        if response.status_code == 404:
            response.failure("Health endpoint not available")
            self.client.get(main_url, name=f"Service Check - {service}")
        else:
            response.success()


class HighLoadUser(EcommerceUser):
    """Usuario para pruebas de alta carga"""
    wait_time = between(1, 3)
    
    @task(15)
    def intensive_browsing(self):
        """Navegación intensiva"""
        self.browse_products_catalog()
        time.sleep(0.5)
        self.browse_categories()
        self.browse_orders()