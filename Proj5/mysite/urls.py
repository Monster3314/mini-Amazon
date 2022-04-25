from django.urls import path
from .views import *
from . import views

urlpatterns = [
    path('', views.home, name='home'),
    path('home/<int:pk>',ProductsDetailView.as_view(), name='product_detail'),
    path('add_to_cart/<int:product_id>', views.add_to_cart, name='add_to_cart'),
    path('delete_order/<int:order_id>', views.delete_order, name='delete_order'),
    path('delete_package/<int:package_id>', views.delete_package, name='delete_package'),
    path('shopping_cart/<int:pk>',PackageDetailView.as_view(), name='package_detail'),
    path('home/<int:pk>/comment', AddCommentView.as_view(), name='add_comments'),
    path('decrease_the_number/<int:order_id>', views.decrease, name='decrease_the_number'),
    path('add_the_number/<int:order_id>', views.add, name='add_the_number'),
]