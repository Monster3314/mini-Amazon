# Generated by Django 4.0.1 on 2022-04-22 15:13

from django.db import migrations, models


class Migration(migrations.Migration):

    dependencies = [
        ('user', '0002_remove_profile_image_profile_phone_profile_ups_id'),
    ]

    operations = [
        migrations.RemoveField(
            model_name='profile',
            name='ups_id',
        ),
        migrations.AddField(
            model_name='profile',
            name='account_money',
            field=models.FloatField(default=0.01),
        ),
        migrations.AddField(
            model_name='profile',
            name='card_info',
            field=models.BigIntegerField(default=0),
        ),
    ]