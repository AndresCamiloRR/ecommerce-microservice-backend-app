from locust import HttpUser, task, between
import random
from datetime import datetime
import dotenv
import os # Importar os

class Test(HttpUser):
    wait_time = between(1, 2)

    # Attempt to get HOST from environment variable (typically set by Docker)
    host = os.getenv('HOST')

    if not host:
        # If HOST environment variable is not set, try to load from .env file
        # This is useful for local development runs without Docker or if HOST is not in the environment
        host_from_dotenv = dotenv.get_key('.env', 'HOST')
        if host_from_dotenv:
            host = host_from_dotenv
        else:
            # Fallback to a default host if not found in environment variables or .env file
            host = "http://localhost:8080"
            # Optional: print a warning that a default is being used
            # print(f"Warning: HOST not set via environment or .env. Defaulting to {host}")

    # Caso de uso 1: Consultar los productos
    @task
    def get_products(self):
        self.client.get("/product-service/api/products")

    # Caso de uso 2: Crear ordenes
    @task
    def post_order(self):

        random_order_id = random.randint(1000, 99999)

        self.client.post(
            "/order-service/api/orders",
            json={
                "orderId": random_order_id,
                "orderDate": "23-05-2025__02:05:55:547092",
                "orderDesc": "lorem ipsum dolor sit amet",
                "orderFee": 1000,
                "cart": {
                    "cartId": 1,
                    "userId": 1
                }
            }
        )

    # Caso de uso 3: Solicitar envíos
    @task
    def post_shipping(self):

        self.client.post(
            "/shipping-service/api/shippings",
           json={
                "productId": 1,
                "orderId": 1,
                "orderedQuantity": 2,
                "product": {
                    "productId": 1,
                    "productTitle": "asus",
                    "imageUrl": "xxx",
                    "sku": "dfqejklejrkn",
                    "priceUnit": 0.0,
                    "quantity": 50
                },
                "order": {
                    "orderId": 1,
                    "orderDate": "23-05-2025__02:05:55:547092",
                    "orderDesc": "lorem ipsum dolor sit amet",
                    "orderFee": 1000
                }
            }
        )

    # Caso de uso 4: Añadir productos a favoritos
    @task
    def post_favorites(self):

        self.client.post(
            "/favourite-service/api/favourites",
            json={
                "userId": 1,
                "productId": 1,
                "likeDate": "23-05-2025__02:05:55:547092",
                "user": {
                    "userId" : 1,
                    "firstName" : "selim",
                    "lastName" : "horri",
                    "imageUrl" : "https://bootdey.com/img/Content/avatar/avatar7.png",
                    "email" : "springxyzabcboot@gmail.com",
                    "phone" : "+21622125144"
                    },
                "product" : {
                    "productId" : 3,
                    "productTitle" : "Armani",
                    "imageUrl" : "xxx",
                    "sku" : "fjdvf",
                    "priceUnit" : 0.0,
                    "quantity" : 50
                }
            }
        )


    # Caso de uso 5: Hacer pagos
    @task
    def post_payment(self):

        random_payment_id = random.randint(1000, 99999)

        self.client.post(
            "/payment-service/api/payments",
            json={
                "paymentId" : random_payment_id,
                "isPayed" : False,
                "paymentStatus" : "IN_PROGRESS",
                "order" : {
                "orderId" : 1,
                "orderDate" : "24-05-2025__21:19:26:616072",
                "orderDesc" : "init",
                "orderFee" : 5000.0
                }
            }
        )


    # Caso de uso 6: Registrar usuarios (Sin crear credenciales)
    @task
    def post_user(self):

        random_user_id = random.randint(1000, 99999)

        random_credential_id = random.randint(1000, 99999)

        self.client.post(
            "/user-service/api/users",
            json={
                "userId" : random_user_id,
                    "firstName" : "user",
                    "lastName" : "hivemind",
                    "imageUrl" : "https://bootdey.com/img/Content/avatar/avatar8.png",
                    "email" : "springxyzabcboot@gmail.com",
                    "phone" : "+21622125144",
                    "credential" : {
                        "credentialId" : random_credential_id,
                        "username" : "user",
                        "password" : "usuario",
                        "roleBasedAuthority" : "ROLE_USER",
                        "isEnabled" : True,
                        "isAccountNonExpired" : True,
                        "isAccountNonLocked" : True,
                        "isCredentialsNonExpired" : True
                    }
            }
        )

