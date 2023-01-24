package to.msn.wings.x3033092lastkadai01;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentText;
import com.google.firebase.ml.vision.document.FirebaseVisionDocumentTextRecognizer;

import java.io.IOException;
import java.io.InputStream;

//参考文献：Firebaseの追加方法について
//Android プロジェクトに Firebase を追加する
//(https://firebase.google.com/docs/android/setup?hl=ja)
public class MainActivity extends AppCompatActivity {
    TextView textView; //文字認識で読み込んだテキストをここで出力する

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); //activity_mainをレイアウトにセットする
        Button bt_sl = findViewById(R.id.button); //画像選択をするためのボタンを設定する
        textView=findViewById(R.id.textView); //文字認識の結果を表示するtext viewの設定
        //画像選択のためのボタンを押したときのイベント処理
        bt_sl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(); //画面を遷移するためのオブジェクト
                intent.setAction(Intent.ACTION_PICK); //オブジェクトの行動を定義
                                                      // （遷移先からデータを取得する）
                intent.setType("image/*"); //取得するデータのタイプを指定(画像）
                //startActivityForResultを用いることで遷移先のデータを保持したまま元の画面に戻ることができる
                startActivityForResult(intent,0); //指定したintentの情報と
                                                             // request code=0をonActivityResultに渡して
                                                             //画像の保存場所へ遷移を開始する
            }
        });

        //TranslateActivityに移動するためのボタン
        Button bt_cmr = findViewById(R.id.translate_button);
        //そのボタンを押したときのイベント処理
        bt_cmr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //画面遷移のためのオブジェクト
                Intent intent = new Intent(getApplication(), TranslateActivity.class);
                //画面の遷移のみを行う
                // TranslateActivityに移動する
                startActivity(intent);
            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    //遷移先からデータを受け取って、そのデータを画像に変換し、その画像から文字認識を行うためのメソッド
    //参考文献：画像をフォルダから読み込んで文字認識をする方法について
    //FirebaseのMLkitで文字認識をいろいろ試してみた(https://www.techceed-inc.com/engineer_blog/3526/)
    protected void onActivityResult(int requestCode, final int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //画像の保存先へ移動した時の処理
        if (requestCode == 0) {
            if (resultCode == Activity.RESULT_OK) {
                //画像選択で得たURIをinput streamに紐づけるためのオブジェクト
                ContentResolver contentResolver = getContentResolver();
                //Bitmapを生成するためのオブジェクト
                InputStream inputStream = null;
                //受けとった画像データの向きから、画像の傾きを示す値
                //参考文献：画像表示について
                //Androidアプリ開発 Exif 画像のExif情報を取得する
                //(http://java-lang-programming.com/ja/articles/84)
                int orientation = ExifInterface.ORIENTATION_UNDEFINED;
                //orientationの値からその角度を0,90,180,270の4パターンで取得する値
                //この値をMatrixに代入することで画像を正しい向きで表示できるようにする
                int rotation;
                try {
                    //受け取ったURIをBitmapへ変換するためにinput streamに紐づける
                    inputStream = contentResolver.openInputStream(data.getData());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //input streamからBitmapを生成する
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                try {
                    //同じURIをinput streamにもう一度紐づける
                    //Bitmapの変換にinput streamを用いたら、input streamの情報が正しく得られなかったため、
                    //もう一度、URIの紐づけを行う
                    //このinput streamから画像の傾きの情報を取得する
                    inputStream = contentResolver.openInputStream(data.getData());
                    //画像ファイルのExifタグを読み取るオブジェクト
                    ExifInterface exifInterface = new ExifInterface(inputStream);
                    //ExifInterfaceによって、画像の傾きを取得
                    orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //ログでorientationの値を確認する
                //正しい画像情報を取得できているかの確認も込み
                Log.d("Exif", "orientation" + orientation);
                //orientationの値によってrotationの値を決定する
                switch (orientation) {
                    //画像が90度傾いている場合
                    case ExifInterface.ORIENTATION_ROTATE_90:
                    case ExifInterface.ORIENTATION_TRANSPOSE:
                        rotation = 90;
                        break;
                        //画像が180度傾いている場合
                    case ExifInterface.ORIENTATION_ROTATE_180:
                    case ExifInterface.ORIENTATION_FLIP_VERTICAL:
                        rotation = 180;
                        break;
                        //画像が270度傾いている場合
                    case ExifInterface.ORIENTATION_ROTATE_270:
                    case ExifInterface.ORIENTATION_TRANSVERSE:
                        rotation = 270;
                        break;
                        //正常であれば、傾き0
                    default:
                        rotation = 0;
                }
                //ログで傾きの角度を確認する
                Log.d("Exif", "rotation" + rotation);
                //Bitmapの傾きを変更するためのオブジェクト
                Matrix transformMatrix = new Matrix();
                //rotationの値をmatrixに設定する
                transformMatrix.setRotate(rotation);
                try {
                    //matrixを引数で渡して傾きが修正されたBitmapを生成する
                    Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), transformMatrix, true);
                    //bitmapが傾いている場合
                    // 元のbitmapと新しく生成したbitmapを比較
                    if (!bitmap.sameAs(converted)) {
                        bitmap = converted; //新しいbitmapに変更
                    }
                } catch (OutOfMemoryError error) {
                    //bitmapの変更ができていなかったら、エラーを通知する
                    Log.e("", "transformBitmap: ", error);
                }
                //image viewを設定する
                ImageView imageView = findViewById(R.id.imageView);
                //image viewにbitmapを表示
                imageView.setImageBitmap(bitmap);
                try {
                    //input streamの値をリセット
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //ここからMLKitの処理を追記
                //文字認識のための画像をbitmapから取得する
                FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
                //画像から文字認識を行うオブジェクトを生成
                FirebaseVisionDocumentTextRecognizer detector = FirebaseVision.getInstance()
                        .getCloudDocumentTextRecognizer();
                //imageから文字認識を行う
                detector.processImage(image)
                        //文字認識が成功した場合
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionDocumentText>() {
                            @Override
                            public void onSuccess(FirebaseVisionDocumentText result) {
                                //認識したテキストをString型で取得する
                                String resultText = result.getText();
                                //そのテキストをtext viewに表示する
                                textView.setText(resultText);
                                //正しく文字認識されていることをログで表示する
                                Log.d("text",resultText);
                            }
                        })
                        //文字認識が失敗した場合
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //ログで文字認識できてないことを通知する
                                Log.d("Error","Not Recognition");
                            }
                        });
            }
        }
    }
}

