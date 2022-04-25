from audioop import reverse

from django.db import models
from django.contrib.auth.models import User
from django.utils.timezone import now



# The category of different items.
class Category(models.Model):
    category = models.CharField(max_length=100, blank=False, null=False)

    def __str__(self):
        return self.category


# warehouse address

class Warehouse(models.Model):
    address_x = models.IntegerField(default=-1)
    address_y = models.IntegerField(default=-1)

    def __str__(self):
        return f'({self.address_x}, {self.address_y})'


class Product(models.Model):
    description = models.CharField(max_length=100, blank=False, null=False)
    inventory = models.IntegerField(default=1)
    category = models.ForeignKey(Category, on_delete=models.SET_NULL, null=True)
    price = models.FloatField(default=0.01, blank=False, null=False)
    img = models.CharField(max_length=100, default="/static/image/images.png")

    # different warehouse have different product
    warehouse = models.ForeignKey(Warehouse, on_delete=models.SET_NULL, null=True)

    def __str__(self):
        return self.description

    # def get_absolute_url(self):
    #     return reverse('product:detail', kwargs={'product_id': self.pk})


#one package for one buyer, contained sevral products
class Package(models.Model):
    owner = models.ForeignKey(User, on_delete=models.CASCADE)
    warehouse = models.ForeignKey(Warehouse, on_delete=models.SET_NULL, null=True)
    # packing
    # packed
    # loading
    # loaded
    # delivering,
    # delivered
    status = models.CharField(max_length=100, default="created")
    deliver_add_x = models.IntegerField(blank=False)
    deliver_add_y = models.IntegerField(blank=False)
    ups_id = models.IntegerField(blank=False )
    # used to find the accurate package for the same user
    create_t = models.DateTimeField(default=now)
    truck_id = models.BigIntegerField(blank=True, null=True)




#use in the shopping cart function
class Order(models.Model):
    buyer = models.ForeignKey(User, on_delete=models.CASCADE)
    item = models.ForeignKey(Product, on_delete=models.SET_NULL, null=True)
    item_num = models.IntegerField(default=1)
    package = models.ForeignKey(Package, on_delete=models.CASCADE, null=True, blank=True)
    is_pay = models.BooleanField(default='False')

    # the total price for the order
    def total(self):
        return self.item_num * self.item.price


    # def __str__(self):
    #     return f'{self.buyer.username}'



class Trend(models.Model):
    product = models.ForeignKey(Product, on_delete=models.SET_NULL, null=True)
    count = models.IntegerField(default=1)


class ProductComment(models.Model):
    user_name = models.CharField(max_length=100)
    body = models.TextField("comment content")
    created_time = models.DateTimeField(default=now)
    product = models.ForeignKey(Product, related_name='comments', on_delete=models.SET_NULL, null=True)

    def __str__(self):
        return self.body[:50]

