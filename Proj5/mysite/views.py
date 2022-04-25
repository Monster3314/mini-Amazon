from django.shortcuts import render, redirect, get_object_or_404
from django.http import HttpResponse
from django.db.models import Q, F
from django.core.exceptions import ObjectDoesNotExist
from .helper import Send_pack_to_backend, calculate_warehouse
from .models import *
from django.contrib.auth.decorators import login_required
from django.contrib import messages
from django.views.generic import DetailView, CreateView, ListView
from user.forms import *
from .forms import *
from django.core.mail import send_mail
from django.conf import settings
from django.utils import timezone
from django.core.paginator import Paginator
from django.db.models import Max
from django.urls import reverse


create_time = timezone.now()


def home(request):
    max = Trend.objects.aggregate(Max('count'))
    value, = max.values()
    try:
        profile = Profile.objects.get(user=request.user)
        x = profile.address_x
        y = profile.address_y
        wh_id = calculate_warehouse(x, y)
        list = Product.objects.filter(warehouse_id=wh_id)
        results = Trend.objects.filter(count=value).first()
        context = {
            # 'goods': Product.objects.all(),
            'now': create_time,
            'results': results,
            'wh_id': wh_id,
            'list':list,
            'value': value
        }
        # connect_backend()
        return render(request, 'mysite/home.html', context)
    except Exception:
        list = Product.objects.order_by('description').distinct('description')
        results = Trend.objects.filter(count=value).first()
        paginator = Paginator(list, 4)
        page_number = request.GET.get('page')
        page_obj = paginator.get_page(page_number)
        context = {
            # 'goods': Product.objects.all(),
            'now': create_time,
            'page_obj': page_obj,
            'results': results,
            'value': value
        }
        # connect_backend()
        return render(request, 'mysite/home.html', context)





def search(request):
    if request.method == 'GET':
        query = request.GET.get('q')
        query_c = request.GET.get('category')
        submitbutton = request.GET.get('submit')
        if query is not None:
            lookups = Q(description__icontains=query)
            results = Product.objects.filter(lookups).distinct('description')
            paginator = Paginator(results, 4)
            page_number = request.GET.get('page')
            page_obj = paginator.get_page(page_number)
            context = {'results': page_obj,
                       'now': create_time,
                       'submitbutton': submitbutton}
            return render(request, 'mysite/search.html', context)
        if query_c is not None:
            try:
                q = Category.objects.get(category=query_c)
            except Exception:
                messages.warning(request, f'ooops, no such category exist')
                return redirect('search')
            results = Product.objects.filter(category=q.id)
            paginator = Paginator(results, 4)
            page_number = request.GET.get('page')
            page_obj = paginator.get_page(page_number)
            context = {'results': page_obj,
                       'now': create_time,
                       'submitbutton': submitbutton}
            return render(request, 'mysite/search.html', context)

    return render(request, 'mysite/search.html')





def add_to_cart(request, product_id):
    user_id = request.user.id
    product_id = int(product_id)
    count = 1
    # calculate trend
    product = Product.objects.get(id=product_id)
    if product.inventory < 3:
        messages.warning(request, f'sorry, the product is out of stock!')
        return redirect('home')
    r = Trend.objects.filter(product=product)
    if len(r) >=1 :
        r = r[0]
        r.count = r.count + count
        r.save()
    else :
        Trend.objects.create(product=product,count=count)

    order = Order.objects.filter(buyer=user_id, item=product_id, is_pay=False)
    if len(order) >= 1:  # 购物车有商品
        order = order[0]
        order.item_num = order.item_num + count
        order.save()
    else:  # 购物车没东西
        user = User.objects.get(id=user_id)
        Order.objects.filter(buyer=user_id).create(buyer=user, item=product, item_num=count)
        #todo: ??fix problem
    Product.objects.filter(id=product_id).update(inventory=F('inventory') - count)
    messages.success(request, f'add to cart success !')
    return redirect('shopping_cart')


def shopping_cart(request):
    packages = Package.objects.filter(owner=request.user)
    orders = Order.objects.filter(buyer=request.user, package__isnull=True, is_pay=False)
    total = 0
    for order in orders:
        total = total + order.item.price * order.item_num
    context = {
        'orders': orders,
        'total': total,
        'now': create_time,
        'packages': packages
    }
    return render(request, 'mysite/shopping_cart.html', context)



@login_required
def check_out(request):

    if request.method == 'POST':
        package_form = PackageInfoForm(request.POST or None)
        # todo:判断form的有效
        package_form.instance.owner = request.user
        package_form.instance.create_t = create_time
        package_form.instance.deliver_add_x = request.user.profile.address_x
        package_form.instance.deliver_add_y = request.user.profile.address_y
        orders = Order.objects.filter(buyer=request.user, package__isnull=True, is_pay=False).first()
        package_form.instance.warehouse_id = orders.item.warehouse_id

        if package_form.is_valid():
            # change the account money
            orders = Order.objects.filter(buyer=request.user)
            total = 0
            for order in orders:
                total = total + order.item.price * order.item_num
            user = request.user
            try:
                if user.profile.account_money < total:
                    messages.warning(request, f'You dont have enough money! please go to the profile page to charge money ')
                    return redirect('shopping_cart')
            except Exception:
                messages.warning(request, f'ooops, You dont have profile now, please complete your profile first!')
                return redirect('shopping_cart')
            package_form.save()
            #update user account
            Profile.objects.filter(user=request.user).update(account_money=F('account_money') - total)
            #handle the shopping cart显示部分
            orders = Order.objects.filter(buyer=request.user, package__isnull=True, is_pay=False)
            for order in orders:
                order.package = package_form.instance
                order.is_pay = True
                order.save()
            # todo: connect with the backend

            # ups_id = package_form.cleaned_data["ups_id"]
            # time = package_form.instance.create_t
            # package = Package.objects.get(owner=request.user,ups_id=ups_id,deliver_add_x=address_x, deliver_add_y=address_y, create_t=time)
            Send_pack_to_backend(package_form.instance.id)

            #send_email
            subject = "Thank you for shopping on Amazon"
            msg = 'Your order will deliever in several days\n'
            msg += 'Best, \namazon\n'
            from_email = settings.EMAIL_HOST_USER
            email_list = [user.email, from_email]
            send_mail(subject, msg, from_email, email_list, fail_silently=True)
            messages.success(request, f'A confirmation email has been sent to your account!')
            return render(request, 'mysite/success.html', {'now': create_time})
    else:
        order = Order.objects.filter(buyer=request.user, package__isnull=True, is_pay=False)
        package_form = PackageInfoForm(request.POST or None)
        profile = Profile.objects.get(user=request.user)
        context = {
            'package_form': package_form,
            'now': create_time,
            'orders': order,
            'profile' : profile
        }
        return render(request, 'mysite/check_out.html', context)


def delete_order(request, order_id):
        order = Order.objects.get(pk=int(order_id))
        order.delete()
        messages.success(request, f'Product has been removed from cart!')
        return redirect('shopping_cart')


def delete_package(request, package_id):
    package = Package.objects.get(pk=int(package_id))
    package.delete()
    messages.success(request, f'Order history has been delete!')
    return redirect('shopping_cart')


class ProductsDetailView(DetailView):
    model = Product

    # def get_queryset(self):
    #     product = Product.objects.get(pk=self.request.GET.dict())
    #     return Product.objects.filter(description=product.description)

# class ProductLiStView(ListView):
#     pass
#
# class FilterProductListView(ProductLiStView):
#     def get_queryset(self):




class AddCommentView(CreateView):
    model = ProductComment
    form_class = CommentForm
    template_name = 'add_comments.html'

    def form_valid(self, form):
        form.instance.product_id = self.kwargs['pk']
        return super().form_valid(form)

    def get_success_url(self):
        return reverse('home')


class PackageDetailView(DetailView):
    model = Package


def decrease(request, order_id):
    packages = Package.objects.filter(owner=request.user)
    order_id = int(order_id)
    count = 1
    order = Order.objects.get(buyer=request.user, pk=order_id, package__isnull=True, is_pay=False)
    if order.item_num == count :
        messages.success(request, f'sorry you can not decrease any more!')
        return redirect('shopping_cart')
    else:
        Order.objects.filter(buyer=request.user, pk=order_id, package__isnull=True, is_pay=False).update(item_num=F('item_num') - count)
        Product.objects.filter(id=order.item_id).update(
            inventory=F('inventory') + count)
        orders = Order.objects.filter(buyer=request.user, package__isnull=True, is_pay=False)
        total = 0
        for order in orders:
            total = total + order.item.price * order.item_num
        context = {
            'orders': orders,
            'total': total,
            'now': create_time,
            'packages': packages
        }
        return render(request, 'mysite/shopping_cart.html', context)


def add(request, order_id):
    packages = Package.objects.filter(owner=request.user)
    order_id = int(order_id)
    count = 1
    order = Order.objects.get(buyer=request.user, pk=order_id, package__isnull=True, is_pay=False)
    product = Product.objects.get(id=order.item_id)
    if order.item_num > product.inventory:
        messages.success(request, f'sorry you can not add any more!')
        return redirect('shopping_cart')
    else:
        Order.objects.filter(buyer=request.user, pk=order_id, package__isnull=True, is_pay=False).update(
            item_num=F('item_num') + count)
        Product.objects.filter(id=order.item_id).update(
            inventory=F('inventory') - count)
        orders = Order.objects.filter(buyer=request.user, package__isnull=True, is_pay=False)
        total = 0
        for order in orders:
            total = total + order.item.price * order.item_num
        context = {
            'orders': orders,
            'total': total,
            'now': create_time,
            'packages': packages
        }
        return render(request, 'mysite/shopping_cart.html', context)