from django.contrib import admin

from .models import *

admin.site.register(Category)
admin.site.register(Product)
admin.site.register(Warehouse)
admin.site.register(Order)
admin.site.register(Package)
admin.site.register(Trend)
admin.site.register(ProductComment)


