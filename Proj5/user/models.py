from django.db import models
from django.contrib.auth.models import User

class Profile(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE)
    phone = models.BigIntegerField(default=0)
    card_info = models.BigIntegerField(default=0)
    account_money = models.FloatField(default=0.01)
    address_x = models.BigIntegerField(default=0)
    address_y = models.BigIntegerField(default=0)


    def __str__(self):
        return f'{self.user.username}Profile'
