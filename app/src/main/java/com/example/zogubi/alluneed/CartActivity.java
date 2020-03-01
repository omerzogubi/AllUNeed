package com.example.zogubi.alluneed;

import android.content.DialogInterface;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.zogubi.alluneed.Model.Cart;
import com.example.zogubi.alluneed.Prevalent.Prevalent;
import com.example.zogubi.alluneed.ViewHolder.CartViewHolder;
import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class CartActivity extends AppCompatActivity {

    private RecyclerView recyclerView ;
    private RecyclerView.LayoutManager layoutManager;
    private Button nextProcessBtn;
    private TextView totalPrice , message1TextView;
    private int overTotalPrice = 0 ;
    DatabaseReference cartListRef;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart);

        nextProcessBtn = (Button) findViewById(R.id.next_process_btn);
        totalPrice = (TextView) findViewById(R.id.total_price);
        recyclerView = findViewById(R.id.cart_list);
        recyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        message1TextView = (TextView) findViewById(R.id.message1);

        cartListRef = FirebaseDatabase.getInstance().getReference().child("Cart List").child("User View");
        cartListRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (!dataSnapshot.hasChild(Prevalent.currentOnlineUser.getPhone())){
                    nextProcessBtn.setEnabled(false);
                }else{
                    nextProcessBtn.setEnabled(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

        nextProcessBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(CartActivity.this , ConfirmOrderAddressActivity.class);
                intent.putExtra("Total Price", String.valueOf(overTotalPrice));
                startActivity(intent);
                finish();
            }
        });


    }


    @Override
    protected void onStart() {
        super.onStart();

       CheckOrderState();

        cartListRef = FirebaseDatabase.getInstance().getReference().child("Cart List");

        FirebaseRecyclerOptions<Cart> options = new FirebaseRecyclerOptions.Builder<Cart>()
                .setQuery(cartListRef.child("User View")
                .child(Prevalent.currentOnlineUser.getPhone()).child("Products"), Cart.class).build();

        FirebaseRecyclerAdapter<Cart,CartViewHolder> adapter = new FirebaseRecyclerAdapter<Cart, CartViewHolder>(options) {
            @Override
            protected void onBindViewHolder(@NonNull CartViewHolder holder, int position, @NonNull final Cart model) {

                holder.cartProductQuantity.setText("Quantity= " + model.getQuantity());
                holder.cartProductPrice.setText("Price = " + model.getPrice() +"TL");
                holder.cartProductName.setText(model.getPname());

                int oneTypeProductTPrice = ((Integer.valueOf(model.getPrice())))*Integer.valueOf(model.getQuantity());
                overTotalPrice+= oneTypeProductTPrice;
                totalPrice.setText("Total Price = "+String.valueOf(overTotalPrice) + "₺");


                //deleting products from Cart

                holder.itemView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CharSequence options [] = new CharSequence[]{"Edit" ,"Remove"};
                        AlertDialog.Builder builder = new AlertDialog.Builder(CartActivity.this);
                        builder.setTitle("Product Options");
                        builder.setItems(options, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int i ) {
                                if (i == 0){
                                    Intent intent = new Intent(CartActivity.this , ProductDetailsActivity.class);
                                    intent.putExtra("pid" , model.getPid());
                                    intent.putExtra("quantityEdit" , Integer.valueOf(model.getQuantity()));

                                    startActivity(intent);
                                }
                                if (i ==1 ) {

                                    cartListRef.child("User View").child(Prevalent.currentOnlineUser.getPhone())
                                            .child("Products").child(model.getPid()).removeValue()
                                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                                @Override
                                                public void onComplete(@NonNull Task<Void> task) {

                                                    if (task.isSuccessful()){
                                                        Toast.makeText(CartActivity.this, "Item Removed Successfully.", Toast.LENGTH_SHORT).show();

                                                        Intent intent = new Intent(CartActivity.this , HomeActivity.class);
                                                        startActivity(intent);
                                                    }else{
                                                        Toast.makeText(CartActivity.this, "NetWork Error,   Please Try Again Later", Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                    cartListRef.child("Admin View").child(Prevalent.currentOnlineUser.getPhone())
                                            .child("Products").child(model.getPid()).removeValue();




                                }

                            }
                    });
                        builder.show();

                    }
                });

            }

            @NonNull
            @Override
            public CartViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
                View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.cart_item_layout , parent , false);
                CartViewHolder holder = new CartViewHolder(view);
                return holder;


            }
        };

        recyclerView.setAdapter(adapter);
        adapter.startListening();
    }


    private void CheckOrderState (){
        DatabaseReference orderRef;
            orderRef = FirebaseDatabase.getInstance().getReference().child("Orders")
                    .child(Prevalent.currentOnlineUser.getPhone());

        orderRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.exists()){
                    String shipmentState  = dataSnapshot.child("state").getValue().toString();
                    String userName  = dataSnapshot.child("name").getValue().toString();
                    if (shipmentState.equals("shipped")) {

                        totalPrice.setText("Order Shipped !");
                        recyclerView.setVisibility(View.GONE);
                        message1TextView.setText("Dear " + userName + "\n Your Order Has Been shipped Succesfully , soon it will be in your Hands :)");
                        message1TextView.setVisibility(View.VISIBLE);
                        nextProcessBtn.setVisibility(View.GONE);
                       // Toast.makeText(CartActivity.this, "You Can't Purchase More Orders Until You Receive The Current Order", Toast.LENGTH_LONG).show();


                    }else if (shipmentState.equals("not shipped")) {

                        totalPrice.setText("Waiting For Shipping");
                        recyclerView.setVisibility(View.GONE);
                        message1TextView.setText("Dear " + userName + "\n After Admin's Verification ,Your Order Will Be Handled And Shipped");
                        message1TextView.setVisibility(View.VISIBLE);
                        nextProcessBtn.setVisibility(View.GONE);
                       // Toast.makeText(CartActivity.this, "Sorry, You Only Can Purchase More Products When Your Current Order got Shipped", Toast.LENGTH_LONG).show();



                    }else{

                    }


                }

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });




    }
}
