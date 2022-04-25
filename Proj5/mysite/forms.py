from django import forms

from .models import *



class PackageInfoForm(forms.ModelForm):

    class Meta:
        model = Package
        fields = ['status',  'ups_id']


class WarehouseForm(forms.ModelForm):

    class Meta:
        model = Warehouse
        fields = '__all__'


class CommentForm(forms.ModelForm):

    class Meta:
        model = ProductComment
        fields = ['user_name', 'body']

